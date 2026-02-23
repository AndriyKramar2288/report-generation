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
        log.debug("Course object model successfully formed");

        for (int i = 1; i <= cos.getLabs().size(); i++) {
            log.debug("Starting processing of laboratory work #{}", i);
            var lab = cos.getLabs().get(i - 1);
            Path labRoot = isSameDirectory ? root : root.resolve("lab-" + i);
            String romFileName = isSameDirectory ? "rom" + i + ".md" : "rom.md";

            Files.createDirectories(labRoot);

            if (!lab.getShellCommands().isEmpty()) {
                log.debug("Executing shell commands (count: {}) in directory: {}", lab.getShellCommands().size(), labRoot);
                var shellResult = shellInteractiveRunner.runAllInOneSession(labRoot, lab.getShellCommands(), true);
                log.debug("Shell execution result: {}", shellResult);
            }

            for (LabModel.LabFile file : lab.getFiles()) {
                createFileWithDirs(labRoot.resolve(file.getName()), file.getContent());
                log.debug("File created: {}", file.getName());
            }

            log.debug("Creating laboratory work object model file");
            Files.writeString(labRoot.resolve(romFileName), lab.getReport(), StandardCharsets.UTF_8);
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                isSameDirectory ? 1 : Runtime.getRuntime().availableProcessors());

        if (isBuild) {
            log.debug("Starting report generation process");
            for (int i = 1; i <= cos.getLabs().size(); i++) {

                int finalI = i;
                Path labRoot = isSameDirectory ? root : root.resolve("lab-" + i);
                String romFileName = isSameDirectory ? "rom" + i + ".md" : "rom.md";

                executor.submit(() -> {
                    log.debug("Generating report #{}", finalI);
                    try {
                        var rom = ReportObjectModel.create(labRoot.resolve(romFileName).toUri(), labRoot);
                        projectionValidator.validate(rom);
                        log.debug("Report object model #{} formed, proceeding to file generation", finalI);
                        reportGenerationFacade.generate(Objects.requireNonNull(rom),
                                getClass().getResourceAsStream("/template.docx"),
                                "report-" + finalI,
                                labRoot,
                                true,
                                true);
                    }
                    catch (Exception e) {
                        log.error("Report generation #{} failed. Error: {}",
                                finalI, e.getMessage());
                    }
                });
            }
        }

        executor.shutdown();
        log.info("Awaiting termination of all generation tasks...");
        try {
            if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.MINUTES)) {
                log.error("Timeout reached. Some reports were not generated.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Execution interrupted", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void givePrompt(Path root) {
        log.debug("Copying default prompt.md to com.xml in: {}", root);
        try (InputStream promptInput = this.getClass().getResourceAsStream("/prompt.md")) {
            if (promptInput != null) {
                Files.copy(promptInput, root.resolve("com.xml"));
                xmlService.generateSchema(root);
            }
        } catch (IOException e) {
            log.error("Failed to copy prompt template", e);
            throw new RuntimeException(e);
        }
    }

    private void createFileWithDirs(Path filePath, String content) throws IOException {
        Path parentDir = filePath.getParent();
        if (parentDir != null && Files.notExists(parentDir)) {
            log.info("Creating missing directories: {}", parentDir);
            Files.createDirectories(parentDir);
        }
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        log.info("File successfully created at: {}", filePath);
    }
}