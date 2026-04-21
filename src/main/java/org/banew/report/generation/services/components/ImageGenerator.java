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
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Singleton
public class ImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private final PropertiesSource propertiesSource;
    private final File outputDir = new File(System.getProperty("java.io.tmpdir"), "report-gen-images");

    public synchronized File generateCodeImage(String inputPath) throws IOException, InterruptedException {

        log.debug("Preparing to capture code image. Input source path: {}", inputPath);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        log.debug("Starting headless browser for rendering in Temp directory...");

        List<String> command = new ArrayList<>();

        if (IS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(new File(propertiesSource.getNpmDir(), "node_modules\\.bin\\carbon-now.cmd").getAbsolutePath());
        } else {
            // Linux/Mac
            command.add(new File(propertiesSource.getNpmDir(), "node_modules/.bin/carbon-now").getAbsolutePath());
        }

        command.add(Objects.requireNonNull(inputPath));
        command.add("--headless");

        ProcessBuilder pb = new ProcessBuilder(command);

        pb.directory(outputDir);
        pb.redirectErrorStream(true);

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
            return trimCarbonBug(generated);
        } else {
            log.error("Carbon-now execution failed with exit code: {}.", exitCode);
            throw new RuntimeException("Process returned non-zero exit code: " + exitCode);
        }
    }

    @Nullable
    private File findGeneratedFile() {
        log.debug("Scanning directory '{}' for newly generated PNG files.", outputDir.getName());
        File[] files = outputDir.listFiles((d, name) -> name.endsWith(".png"));

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

    private File trimCarbonBug(File imageFile) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) return imageFile;

        int width = img.getWidth();
        int height = img.getHeight();

        int[][] rows = new int[height][width];
        for (int y = 0; y < height; y++) {
            img.getRGB(0, y, width, 1, rows[y], 0, width);
        }

        int maxSeqStart = -1;
        int maxSeqLength = 0;
        int currentSeqStart = -1;
        int currentSeqLength = 0;

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

        if (maxSeqLength > 60) {
            int keepPadding = 30;
            int removeCount = maxSeqLength - keepPadding;

            if (removeCount > 0) {
                log.info("Detected carbon-now padding bug. Slicing out {} empty rows...", removeCount);
                int newHeight = height - removeCount;
                BufferedImage newImg = new BufferedImage(width, newHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = newImg.createGraphics();

                int topPartHeight = maxSeqStart + keepPadding;
                g.drawImage(img.getSubimage(0, 0, width, topPartHeight), 0, 0, null);

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