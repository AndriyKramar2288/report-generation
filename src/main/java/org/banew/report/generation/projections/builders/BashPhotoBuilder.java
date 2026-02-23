package org.banew.report.generation.projections.builders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.banew.report.generation.services.components.ShellInteractiveRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Будівельник зображень на основі виводу командної оболонки.
 * Виконує команди та зберігає результат у тимчасовий файл для подальшої візуалізації.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BashPhotoBuilder extends TextContainingPhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(BashPhotoBuilder.class);
    @Valid
    private List<YamlBashRun> runs = new ArrayList<>();
    private boolean hide = true;
    private File tempFile;

    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        log.debug("Initializing interactive shell session for image generation.");
        var resultString = toolsSource.runAllInOneSession(contextPath, runs, hide);

        log.debug("Creating temporary file to store shell session output.");
        tempFile = Files.createTempFile("run", ".shell").toFile();
        Files.writeString(tempFile.toPath(), resultString);

        return tempFile;
    }

    @Override
    public void close() throws Exception {
        log.debug("Cleaning up temporary resources: deleting shell output file.");
        if (tempFile != null && tempFile.exists()) {
            boolean deleted = tempFile.delete();
            log.trace("Temporary file deletion status: {}", deleted);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class YamlBashRun implements ShellInteractiveRunner.BashRun {
        @NotBlank(message = "Раз ви оголосили виклик, то він не може бути пустим!")
        private String command;
        private String input;
    }
}