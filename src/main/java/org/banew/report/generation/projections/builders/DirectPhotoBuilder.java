package org.banew.report.generation.projections.builders;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.services.components.ToolsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Будівельник для використання вже існуючих зображень у звіті.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DirectPhotoBuilder extends PhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(DirectPhotoBuilder.class);
    @NotBlank(message = "Слід обов'язково вказати файл!")
    private String file;

    @Override
    public File build(Path contextPath, ToolsSource toolsSource) throws IOException {
        log.debug("Attempting to locate existing image file: '{}'", file);
        log.debug("Searching within context directory: {}", contextPath);

        File photo = new File(contextPath.toFile(), file);

        log.debug("Verifying file existence for path: {}", photo.getAbsolutePath());
        if (!photo.exists()) {
            log.error("Image file not found: {}", file);
            throw new FileNotFoundException("File " + file + " not found in the specified context.");
        }

        log.debug("Verifying if the path points to a regular file.");
        if (!photo.isFile()) {
            log.error("Target path is not a regular file: {}", photo.getName());
            throw new FileNotFoundException("Path " + file + " exists but is not a regular file.");
        }

        log.debug("Validating file extension for image compatibility.");
        String name = photo.getName().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
            log.warn("Unsupported file format detected: {}", name);
            throw new FileNotFoundException("File " + file + " has an unsupported format. Use PNG, JPG or JPEG.");
        }

        log.info("Image file '{}' successfully validated and ready for embedding.", photo.getName());
        return photo;
    }
}