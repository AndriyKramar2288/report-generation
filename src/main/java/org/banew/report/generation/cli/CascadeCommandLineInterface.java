package org.banew.report.generation.cli;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.banew.report.generation.services.CascadeUsageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;

/**
 * Командний інтерфейс для каскадної генерації звітів.
 * Дозволяє масово створювати структуру папок та генерувати документи на основі COM XML.
 */
@CommandLine.Command(
        name = "cascade",
        description = "Запустити пакетну генерацію звітів на основі XML-специфікації курсу"
)
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CascadeCommandLineInterface implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CascadeCommandLineInterface.class);

    @CommandLine.Option(names = {"-com", "--CourseObjectModel"},
            description = "Шлях до файлу com.xml")
    private File comPath = new File("com.xml");

    @CommandLine.Option(names = {"-b", "--build"},
            description = "Автоматично почати побудову DOCX/PDF після ініціалізації")
    private boolean isBuild;

    @CommandLine.Option(names = {"-same", "--sameDirectory"},
            description = "Генерувати всі об'єкти в поточній директорії без створення підпапок")
    private boolean isSameDirectory;

    private final CascadeUsageFacade cascadeUsageFacade;

    @Override
    public void run() {
        log.debug("Executing 'cascade' subcommand. Target COM path: {}", comPath.getAbsolutePath());
        try {
            Path context = comPath.getAbsoluteFile().getParentFile().toPath();

            // Валідація наявності та читабельності XML файлу
            if (!comPath.exists() || comPath.isDirectory() || !comPath.canRead() || !comPath.getName().endsWith(".xml")) {
                log.warn("Valid COM file not found or inaccessible at: {}. Providing default template.", comPath);
                cascadeUsageFacade.givePrompt(context);
                log.info("Default COM template and XSD schema have been generated in the directory.");
            } else {
                log.info("COM file identified. Starting cascade processing. Build mode: {}, Same directory: {}", isBuild, isSameDirectory);
                cascadeUsageFacade.process(context, isBuild, isSameDirectory);
                log.info("Cascade processing completed successfully.");
            }
        } catch (Exception e) {
            log.error("Critical failure during cascade command execution", e);
            throw new RuntimeException("Cascade execution failed", e);
        }
    }
}