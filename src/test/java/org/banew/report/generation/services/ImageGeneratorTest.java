package org.banew.report.generation.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnabledOnOs(OS.WINDOWS)
class ImageGeneratorTest {

    @Mock
    private PropertiesSource propertiesSource;
    @InjectMocks
    private ImageGenerator imageGenerator;
    private final Set<File> cacheFile = ConcurrentHashMap.newKeySet();

    @AfterEach
    void tearDown() throws IOException {
        cacheFile.forEach(File::delete);
    }

    @Test
    @DisplayName("Вискакує виняток при переданому null")
    void generateCodeImage_nullInput_Exception() throws Exception {
        when(propertiesSource.getNpmDir()).thenReturn(new File(System.getProperty("user.dir"), "npm"));
        assertThrows(NullPointerException.class, () -> imageGenerator.generateCodeImage(null));
    }

    @Test
    @DisplayName("Успішно генерує фото")
    void generateCodeImage_validInput_successResult(@TempDir Path tempDir) throws Exception {

        Path testTextFile = tempDir.resolve("test.txt");
        String testTextFileAbsolutePath = testTextFile.toFile().getAbsolutePath();
        Files.writeString(testTextFile, "Hello World");

        when(propertiesSource.getNpmDir()).thenReturn(new File(System.getProperty("user.dir"), "npm"));

        File generatedPhoto = imageGenerator.generateCodeImage(testTextFileAbsolutePath);
        assertGeneratedPhoto(generatedPhoto);
    }

    @Test
    @DisplayName("Успішно генерує фото при конкурентному доступі")
    void generateCodeImage_concurrentUsage_successResult(@TempDir Path tempDir) throws Exception {

        int cores = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(cores);

        when(propertiesSource.getNpmDir()).thenReturn(new File(System.getProperty("user.dir"), "npm"));

        try {
            for (int i = 0; i < cores; i++) {
                Path testTextFile = tempDir.resolve("test" + i + ".txt");
                String testTextFileAbsolutePath = testTextFile.toFile().getAbsolutePath();
                Files.writeString(testTextFile, "Hello World");

                executorService.submit(() -> {
                    try {
                        File generatedPhoto = imageGenerator.generateCodeImage(testTextFileAbsolutePath);
                        assertGeneratedPhoto(generatedPhoto);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        finally {
            executorService.awaitTermination(60, TimeUnit.SECONDS);
            executorService.shutdown();
        }
    }

    private void assertGeneratedPhoto(File generatedPhoto) {
        assertNotNull(generatedPhoto);
        cacheFile.add(generatedPhoto);
        assertTrue(generatedPhoto.exists());
        assertTrue(generatedPhoto.isFile());
        assertTrue(generatedPhoto.canRead());
        assertTrue(generatedPhoto.getName().endsWith(".png")
                || generatedPhoto.getName().endsWith(".jpeg")
                || generatedPhoto.getName().endsWith(".jpg"));
    }
}