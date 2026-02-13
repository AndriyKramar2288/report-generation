package org.banew.report.generation.cli;

import jakarta.xml.bind.JAXBException;
import org.banew.report.generation.ShellRunner;
import org.banew.report.generation.cascade.XmlUtils;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import org.banew.report.generation.cascade.xml.LabModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
        name = "cascade"
)
public class CascadeCommandLineInterface implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CascadeCommandLineInterface.class);
    @CommandLine.Option(names = {"-com", "--CourseObjectModel"},
            description = "Розташування com.xml")
    private File comPath = new File("com.xml");

    @Override
    public void run() {
        try {
            if (!comPath.exists() || comPath.isDirectory() || !comPath.canRead() || !comPath.getName().endsWith(".xml")) {
                givePrompt();
            }
            else {
                process();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void process() throws JAXBException, IOException {
        CourseObjectModel cos = XmlUtils.unmashallCourseObjectModel(comPath);
        log.debug("Об'єкта модель курсу сформована");

        for (int i = 0; i < cos.getLabs().size(); i++) {
            log.debug("Початок обробки лабки №{}", i);
            var lab = cos.getLabs().get(i);
            Path labRoot = Path.of("lab-" + i);

            for (LabModel.LabFile file : lab.getFiles()) {
                createFileWithDirs(labRoot.resolve(file.getName()), file.getContent());
                log.debug("Створено файл {}", file.getName());
            }

            if (!lab.getShellCommands().isEmpty()) {
                log.debug("Запуск скриптів ({} штуки)", lab.getShellCommands().size());
                var shellResult = ShellRunner.runAllInOneSession(labRoot, lab.getShellCommands(), true);
                log.debug("Результат виконання скрипта: {}", shellResult);
            }

            log.debug("Створення файлу об'єктної моделі лабки");
            Files.writeString(labRoot.resolve("lom.md"), lab.getReport(), StandardCharsets.UTF_8);
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
