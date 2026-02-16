package org.banew.report.generation.services;

import com.google.inject.Guice;
import org.banew.report.generation.cli.GuiceModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicUsageFacadeIntegrationTest {

    private final BasicUsageFacade basicUsageFacade = Guice
            .createInjector(new GuiceModule())
            .getInstance(BasicUsageFacade.class);

    @Test
    void process_correctInputs_success(@TempDir Path tempDir) throws Exception {
        copyResourceToDir(tempDir, "/lab-example/", "lab4_clean.py");
        copyResourceToDir(tempDir, "/lab-example/", "rom.md");
        copyResourceToDir(tempDir, "/lab-example/", "variant05.csv");
        assertWithDefaultValues(tempDir);
    }

    @Test
    void process_withUserInput_success(@TempDir Path tempDir) throws Exception {
        copyResourceToDir(tempDir, "/lab-input-example/", "lab4_clean.py");
        copyResourceToDir(tempDir, "/lab-input-example/", "rom.md");
        assertWithDefaultValues(tempDir);
    }

    @Test
    void process_shellFailure_exceptionThrown(@TempDir Path tempDir) throws Exception {
        copyResourceToDir(tempDir, "/lab-example/", "lab4_clean.py");
        copyResourceToDir(tempDir, "/lab-example/", "rom.md");
        // variant05.csv is absent, so shell-image-builder has to failure
        assertThrows(Exception.class, () -> assertWithDefaultValues(tempDir));
    }

    private void assertWithDefaultValues(Path tempDir) throws Exception {
        basicUsageFacade.process(tempDir.resolve("rom.md").toUri(),
                tempDir,
                tempDir.resolve("result").toFile(),
                getClass().getResourceAsStream("/template.docx"),
                true,
                true);

        assertTrue(new File(tempDir.toFile(), "result.docx").exists());
        assertTrue(new File(tempDir.toFile(), "result.pdf").exists());
    }

    private void copyResourceToDir(Path dir, String resourceFolder, String fileName) throws Exception {
        Files.copy(Path.of(Objects.requireNonNull(
                getClass().getResource(resourceFolder + fileName)).toURI()), dir.resolve(fileName));
    }
}