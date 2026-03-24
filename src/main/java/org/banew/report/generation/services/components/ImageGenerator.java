package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Дозволяє генерувати файні фото з вмістом деякого файлу.
 * <b>Thread-Safe</b>
 */
@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class ImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private final PropertiesSource propertiesSource;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * Генерує файл зображення з вмістом деякого файлу та автоматично обрізає баговані відступи.
     * @param inputPath абсолютний шлях до будь-якого текстового файлу
     * @return вихідне зображення
     * @throws IOException у разі, якщо не буде знайдено фото
     */
    public synchronized File generateCodeImage(String inputPath) throws IOException, InterruptedException {

        log.debug("Preparing to capture code image. Input source path: {}", inputPath);
        log.debug("NPM working directory: {}", propertiesSource.getNpmDir().getAbsolutePath());

        log.debug("Invoking npx carbon-now. Starting headless browser for rendering...");

        List<String> command = new ArrayList<>();
        if (IS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
        }
        command.add("npx");
        command.add("carbon-now");
        command.add(Objects.requireNonNull(inputPath));
        command.add("--headless");

        ProcessBuilder pb = new ProcessBuilder(command);

        log.debug("Inheriting IO streams to monitor Node.js process output.");
        pb.directory(propertiesSource.getNpmDir());

        log.debug("Starting external process: carbon-now (headless mode).");
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[Node.js] {}", line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            log.debug("Process execution successful. Searching for the generated output file.");
            File generated = Objects.requireNonNull(findGeneratedFile());

            // ВАЖЛИВО: Пропускаємо фотку через наш "хірургічний" обрізувач багів Карбону
            return trimCarbonBug(generated);
        }
        else {
            log.error("Carbon-now execution failed with exit code: {}. Check Node.js environment.", exitCode);
            throw new RuntimeException("Process returned non-zero exit code: " + exitCode);
        }
    }

    /**
     * Обшукує усю {@code propertiesSource.getNpmDir()} з метою знайти щойно згенероване фото
     * @return згенероване фото
     */
    @Nullable
    private File findGeneratedFile() {
        log.debug("Scanning directory '{}' for newly generated PNG files.", propertiesSource.getNpmDir().getName());
        File[] files = propertiesSource.getNpmDir().listFiles((d, name) -> name.endsWith(".png"));

        if (files == null || files.length == 0) {
            log.warn("No generated image files found in the target directory.");
            return null;
        }

        log.debug("Found {} potential files. Selecting the most recent one based on modification time.", files.length);
        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }

        log.info("Successfully identified the generated image: '{}'", latest.getName());
        return latest;
    }

    /**
     * Хірургічний алгоритм для видалення "страшного відступу" (багу carbon-now).
     * Шукає гігантський блок однакових горизонтальних ліній пікселів і вирізає його,
     * зберігаючи тіні, закруглені кути та рамки вікна на дні.
     */
    private File trimCarbonBug(File imageFile) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) return imageFile;

        int width = img.getWidth();
        int height = img.getHeight();

        // 1. Кешуємо всі рядки пікселів для швидкого порівняння
        int[][] rows = new int[height][width];
        for (int y = 0; y < height; y++) {
            img.getRGB(0, y, width, 1, rows[y], 0, width);
        }

        int maxSeqStart = -1;
        int maxSeqLength = 0;
        int currentSeqStart = -1;
        int currentSeqLength = 0;

        // 2. Шукаємо найдовшу послідовність АБСОЛЮТНО однакових рядків
        // Це і є та сама розтягнута порожня ділянка фону
        for (int y = 1; y < height; y++) {
            if (Arrays.equals(rows[y], rows[y - 1])) {
                if (currentSeqLength == 0) {
                    currentSeqStart = y - 1;
                    currentSeqLength = 2;
                } else {
                    currentSeqLength++;
                }
            } else {
                if (currentSeqLength > maxSeqLength) {
                    maxSeqLength = currentSeqLength;
                    maxSeqStart = currentSeqStart;
                }
                currentSeqLength = 0;
            }
        }
        if (currentSeqLength > maxSeqLength) {
            maxSeqLength = currentSeqLength;
            maxSeqStart = currentSeqStart;
        }

        // 3. Якщо є порожній блок висотою більше 60 пікселів - це точно баг Карбону
        if (maxSeqLength > 60) {
            int keepPadding = 30; // Залишаємо 30 пікселів фону для естетики (щоб текст не бився в дно)
            int removeCount = maxSeqLength - keepPadding;

            if (removeCount > 0) {
                log.info("Detected carbon-now padding bug. Slicing out {} empty rows...", removeCount);
                int newHeight = height - removeCount;
                BufferedImage newImg = new BufferedImage(width, newHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = newImg.createGraphics();

                // Малюємо верхню частину (весь код + трохи порожнього простору)
                int topPartHeight = maxSeqStart + keepPadding;
                g.drawImage(img.getSubimage(0, 0, width, topPartHeight), 0, 0, null);

                // Малюємо нижню частину (заокруглені кути вікна та тінь), склеюючи їх
                int bottomPartY = maxSeqStart + maxSeqLength;
                int bottomPartHeight = height - bottomPartY;
                g.drawImage(img.getSubimage(0, bottomPartY, width, bottomPartHeight), 0, topPartHeight, null);

                g.dispose();
                ImageIO.write(newImg, "png", imageFile);
                log.info("Image successfully trimmed! The void is gone.");
            }
        }
        return imageFile;
    }
}