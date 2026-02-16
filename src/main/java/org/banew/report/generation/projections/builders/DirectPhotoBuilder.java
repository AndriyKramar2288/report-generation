package org.banew.report.generation.projections.builders;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.services.ToolsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

@EqualsAndHashCode(callSuper = true)
@Data
public class DirectPhotoBuilder extends PhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(DirectPhotoBuilder.class);
    @NotBlank(message = "Слід обов'язково вказати файл!")
    private String file;

    @Override
    public File build(Path contextPath, ToolsSource toolsSource) throws IOException {
        log.debug("Так, блядь, пробуєм надибати готову фотку: '{}'", file);
        log.debug("Риємся в оцій папці: {}", contextPath);

        File photo = new File(contextPath.toFile(), file);

        log.debug("Провіряєм, чи ця фотка взагалі іствує, чи ти нас найобуєш");
        if (!photo.exists()) {
            log.debug("Пізда, файла '{}' тупо нема. Де ти його дівав, сука?", file);
            throw new FileNotFoundException(file + " is not found, блядь!");
        }

        log.debug("Провіряєм, чи це реально файл, а не якась папка чи інша хуйня");
        if (!photo.isFile()) {
            log.debug("Це не файл, це якась залупа: {}", photo.getName());
            throw new FileNotFoundException(file + " is not a file, сука!");
        }

        log.debug("Дивимся на розширення, бо нам треба тіки нормальні картинки, а не всякий кал");
        String name = photo.getName().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
            log.debug("Шо ти мені підсунув? '{}' — це не фотка, це сміття!", name);
            throw new FileNotFoundException(file + " has a wrong format, паскуда!");
        }

        log.debug("Заєбісь, фотка '{},' провірку пройшла. Забирай і не гавкай", photo.getName());
        return photo;
    }
}