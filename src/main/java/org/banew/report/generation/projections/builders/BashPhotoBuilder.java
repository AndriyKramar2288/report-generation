package org.banew.report.generation.projections.builders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.banew.report.generation.ShellRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BashPhotoBuilder extends TextContainingPhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(BashPhotoBuilder.class);
    private List<YamlBashRun> runs = new ArrayList<>();
    private boolean hide = true;
    private File tempFile;

    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        log.debug("Запускаєм сесію, щас буде гарячо");
        var resultString = ShellRunner.runAllInOneSession(contextPath, runs, hide);

        log.debug("Ліпим врємєнний файл, шоб запхати туди цей брєд");
        tempFile = Files.createTempFile("run", ".shell").toFile();
        Files.writeString(tempFile.toPath(), resultString);

        return tempFile;
    }

    @Override
    public void close() throws Exception {
        log.debug("Убираєм за собою гівно, видаляєм файл");
        if (tempFile != null) tempFile.delete();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class YamlBashRun implements ShellRunner.BashRun {
        private String command;
        private String input;
    }
}