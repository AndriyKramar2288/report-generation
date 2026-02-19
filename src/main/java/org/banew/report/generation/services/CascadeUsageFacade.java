package org.banew.report.generation.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import org.banew.report.generation.cascade.xml.LabModel;
import org.banew.report.generation.projections.ReportObjectModel;
import org.banew.report.generation.services.components.ProjectionValidator;
import org.banew.report.generation.services.components.ShellInteractiveRunner;
import org.banew.report.generation.services.components.XmlService;
import org.banew.report.generation.services.reports.ReportGenerationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CascadeUsageFacade {

    private final ReportGenerationFacade reportGenerationFacade;
    private final ShellInteractiveRunner shellInteractiveRunner;
    private final XmlService xmlService;
    private final ProjectionValidator projectionValidator;

    private static final Logger log = LoggerFactory.getLogger(CascadeUsageFacade.class);

    public void process(Path root, boolean isBuild, boolean isSameDirectory) throws JAXBException, IOException {
        CourseObjectModel cos = xmlService.unmashallCourseObjectModel(root.resolve("com.xml").toFile());
        projectionValidator.validate(cos);
        log.debug("Об'єкта модель курсу сформована");

        for (int i = 1; i <= cos.getLabs().size(); i++) {
            log.debug("Початок обробки лабки №{}", i);
            var lab = cos.getLabs().get(i - 1);
            Path labRoot = isSameDirectory ? root : root.resolve("lab-" + i);
            String romFileName = isSameDirectory ? "rom" + i + ".md" : "rom.md";

            Files.createDirectories(labRoot);

            if (!lab.getShellCommands().isEmpty()) {
                log.debug("Запуск скриптів ({} штуки) за директорією {}", lab.getShellCommands().size(), labRoot);
                var shellResult = shellInteractiveRunner.runAllInOneSession(labRoot, lab.getShellCommands(), true);
                log.debug("Результат виконання скрипта: {}", shellResult);
            }

            for (LabModel.LabFile file : lab.getFiles()) {
                createFileWithDirs(labRoot.resolve(file.getName()), file.getContent());
                log.debug("Створено файл {}", file.getName());
            }

            log.debug("Створення файлу об'єктної моделі лабки");
            Files.writeString(labRoot.resolve(romFileName), lab.getReport(), StandardCharsets.UTF_8);
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                isSameDirectory ? 1 : Runtime.getRuntime().availableProcessors());

        if (isBuild) {
            log.debug("Початок побудови звітів");
            for (int i = 1; i <= cos.getLabs().size(); i++) {

                int finalI = i;
                Path labRoot = isSameDirectory ? root : root.resolve("lab-" + i);
                String romFileName = isSameDirectory ? "rom" + i + ".md" : "rom.md";

                executor.submit(() -> {
                    log.debug("Початок побудови звіту №{}", finalI);
                    try {
                        var rom = ReportObjectModel.create(labRoot.resolve(romFileName).toUri(), labRoot);
                        projectionValidator.validate(rom);
                        log.debug("Об'єктна модель звіту №{} побудована, переходимо до побудови файлу", finalI);
                        reportGenerationFacade.generate(Objects.requireNonNull(rom),
                                getClass().getResourceAsStream("/template.docx"),
                                "report-" + finalI,
                                labRoot,
                                true,
                                true);
                    }
                    catch (Exception e) {
                        log.error("Побудова звіту №{} провалилась! Помилка: {}",
                                finalI, e.getMessage());
                    }
                });
            }
        }

        executor.shutdown();
        log.info("Чекаємо на завершення формування всіх звітів...");
        try {
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.MINUTES)) {
                log.error("Час вийшов, а звіти так і не добудувалися. Якась содомія...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void givePrompt(Path root) {
        try (InputStream promptInput = this.getClass().getResourceAsStream("/prompt.md")) {
            if (promptInput != null) {
                Files.copy(promptInput, root.resolve("com.xml"));
                xmlService.generateSchema(root);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFileWithDirs(Path filePath, String content) throws IOException {

        Path parentDir = filePath.getParent();

        if (parentDir != null && Files.notExists(parentDir)) {
            log.info("Створюємо відсутні директорії: {}", parentDir);
            Files.createDirectories(parentDir);
        }

        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        log.info("Файл успішно створено за шляхом: {}", filePath);
    }
}
