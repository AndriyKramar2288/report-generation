package org.banew.report.generation.services;

import com.google.inject.Guice;
import org.banew.report.generation.cli.GuiceModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CascadeUsageFacadeIntegrationTest {

    private final CascadeUsageFacade cascadeUsageFacade = Guice
            .createInjector(new GuiceModule())
            .getInstance(CascadeUsageFacade.class);

    @Test
    void process_comFilePresentAndIsNotBuild_successFolderCreation(@TempDir Path root) throws Exception {

        Files.copy(Path.of(getClass().getResource("/com-example.xml").toURI()),
                root.resolve("com.xml"));

        cascadeUsageFacade.process(root.toFile(), false);
    }

    @Test
    void givePrompt() {
    }
}