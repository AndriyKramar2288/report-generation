package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
     * Генерує файл зображення з вмістом деякого файлу
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
            return Objects.requireNonNull(findGeneratedFile());
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
}