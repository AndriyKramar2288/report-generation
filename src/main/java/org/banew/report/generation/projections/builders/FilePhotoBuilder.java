package org.banew.report.generation.projections.builders;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePhotoBuilder extends TextContainingPhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(FilePhotoBuilder.class);
    @NotBlank(message = "Слід обов'язково вказати назву файлу!")
    private String name;

    /**
     * Виконує рекурсивний пошук файлу за назвою в заданій директорії.
     */
    private static File findFileContent(String fileName, Path root) {
        log.debug("Initiating file search in directory: {}", root);
        log.debug("Target file name: '{}'", fileName);

        try (Stream<Path> stream = Files.walk(root)) {
            log.debug("Stream initialized, scanning file tree for matches...");

            Optional<Path> foundFile = stream
                    .filter(path -> {
                        boolean isFile = Files.isRegularFile(path);
                        if (!isFile) {
                            log.debug("Skipping non-regular file/directory: {}", path.getFileName());
                        }
                        return isFile;
                    })
                    .filter(path -> {
                        String currentName = path.getFileName().toString();
                        boolean match = currentName.equals(fileName);
                        if (match) {
                            log.info("File match found: {}", path.toAbsolutePath());
                        }
                        return match;
                    })
                    .findFirst();

            if (foundFile.isPresent()) {
                log.debug("Search completed. File is ready for further processing.");
                return foundFile.get().toFile();
            } else {
                log.warn("Search finished. File '{}' not found in context '{}'.", fileName, root);
                return null;
            }
        } catch (IOException e) {
            log.error("IO error occurred during file system traversal: {}", e.getMessage());
            throw new RuntimeException("Error searching for file: " + fileName, e);
        }
    }

    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        log.debug("Requesting file build for '{}' using context: {}", name, contextPath);
        return findFileContent(name, contextPath);
    }
}