package org.banew.report.generation.cli;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.xml.bind.JAXBException;
import org.banew.report.generation.services.ReportBuilder;
import org.banew.report.generation.services.ShellRunner;
import org.banew.report.generation.cascade.XmlUtils;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import org.banew.report.generation.cascade.xml.LabModel;
import org.banew.report.generation.projections.ReportObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandLine.Command(
        name = "cascade"
)
@Singleton
public class CascadeCommandLineInterface implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CascadeCommandLineInterface.class);

    @CommandLine.Option(names = {"-com", "--CourseObjectModel"},
            description = "Розташування com.xml")
    private File comPath = new File("com.xml");

    @CommandLine.Option(names = {"-b", "--build"}, description = "Почати побудову звітів після створення усіх файлів")
    private boolean isBuild;

    private final ReportBuilder reportBuilder;
    private final ShellRunner shellRunner;

    @Inject
    public CascadeCommandLineInterface(ReportBuilder reportBuilder, ShellRunner shellRunner) {
        this.reportBuilder = reportBuilder;
        this.shellRunner = shellRunner;
    }

    @Override
    public void run() {
        try {
            if (!comPath.exists() || comPath.isDirectory() || !comPath.canRead() || !comPath.getName().endsWith(".xml")) {
                givePrompt();
            } else {
                process();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void process() throws JAXBException, IOException {
        CourseObjectModel cos = XmlUtils.unmashallCourseObjectModel(comPath);
        log.debug("Об'єкта модель курсу сформована");

        for (int i = 1; i <= cos.getLabs().size(); i++) {
            log.debug("Початок обробки лабки №{}", i);
            var lab = cos.getLabs().get(i - 1);
            Path labRoot = Path.of("lab-" + i);

            Files.createDirectories(labRoot);

            if (!lab.getShellCommands().isEmpty()) {
                log.debug("Запуск скриптів ({} штуки) за директорією {}", lab.getShellCommands().size(), labRoot);
                var shellResult = shellRunner.runAllInOneSession(labRoot, lab.getShellCommands(), true);
                log.debug("Результат виконання скрипта: {}", shellResult);
            }

            for (LabModel.LabFile file : lab.getFiles()) {
                createFileWithDirs(labRoot.resolve(file.getName()), file.getContent());
                log.debug("Створено файл {}", file.getName());
            }

            log.debug("Створення файлу об'єктної моделі лабки");
            Files.writeString(labRoot.resolve("rom.md"), lab.getReport(), StandardCharsets.UTF_8);
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        if (isBuild) {
            log.debug("Початок побудови звітів");
            for (int i = 1; i <= cos.getLabs().size(); i++) {

                int finalI = i;
                Path labRoot = Path.of("lab-" + i);

                executor.submit(() -> {
                    log.debug("Початок побудови звіту №{}", finalI);
                    try {
                        var rom = ReportObjectModel.create(labRoot.resolve("rom.md").toUri(), labRoot);
                        log.debug("Об'єктна модель звіту №{} побудована, переходимо до побудови файлу", finalI);
                        reportBuilder.generate(Objects.requireNonNull(rom),
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

    private void createFileWithDirs(Path filePath, String content) throws IOException {

        Path parentDir = filePath.getParent();

        if (parentDir != null && Files.notExists(parentDir)) {
            log.info("Створюємо відсутні директорії: {}", parentDir);
            Files.createDirectories(parentDir);
        }

        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        log.info("Файл успішно створено за шляхом: {}", filePath);
    }

    private void givePrompt() {
        try (InputStream promptInput = this.getClass().getResourceAsStream("/prompt.md")) {
            if (promptInput != null) {
                Files.copy(promptInput, Path.of("com.xml"));
                XmlUtils.generateSchema();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
