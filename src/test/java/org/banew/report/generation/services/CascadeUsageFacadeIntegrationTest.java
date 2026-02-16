package org.banew.report.generation.services;

import com.google.inject.Guice;
import org.banew.report.generation.cli.GuiceModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CascadeUsageFacadeIntegrationTest {

    private final CascadeUsageFacade cascadeUsageFacade = Guice
            .createInjector(new GuiceModule())
            .getInstance(CascadeUsageFacade.class);

    @Test
    void process_comFilePresentAndIsNotBuild_successFolderCreation(@TempDir Path root) throws Exception {

        Files.copy(Path.of(Objects.requireNonNull(
                getClass().getResource("/com-example.xml")).toURI()), root.resolve("com.xml"));

        cascadeUsageFacade.process(root, false);

        int labCount = 2;
        int generatedFilesPerFolder = 3;

        for (int i = 1; i <= labCount; i++) {
            File labFolder = root.resolve("lab-" + i).toFile();
            assertTrue(labFolder.exists());
            assertTrue(labFolder.isDirectory());
            assertEquals(generatedFilesPerFolder, Objects.requireNonNull(labFolder.listFiles()).length);
        }
    }

    @Test
    void process_comFilePresentAndIsBuild_success(@TempDir Path root) throws Exception {

        Files.copy(Path.of(Objects.requireNonNull(
                getClass().getResource("/com-example.xml")).toURI()), root.resolve("com.xml"));

        cascadeUsageFacade.process(root, true);

        int labCount = 2;

        for (int i = 1; i <= labCount; i++) {
            File labFolder = root.resolve("lab-" + i).toFile();
            assertTrue(labFolder.exists());
            assertTrue(labFolder.isDirectory());
            assertNotNull(labFolder.listFiles());
            assertNotEquals(0, Objects.requireNonNull(labFolder.listFiles()).length);
        }

        int pdfCount = (int) Files.walk(root)
                .filter(path -> path.toFile().getName().endsWith(".pdf"))
                .count();
        int docxCount = (int) Files.walk(root)
                .filter(path -> path.toFile().getName().endsWith(".docx"))
                .count();
        assertEquals(2, pdfCount);
        assertEquals(2, docxCount);
    }

    @Test
    void givePrompt(@TempDir Path root) {
        cascadeUsageFacade.givePrompt(root);
        assertEquals(2, Objects.requireNonNull(root.toFile().listFiles()).length);
    }
}