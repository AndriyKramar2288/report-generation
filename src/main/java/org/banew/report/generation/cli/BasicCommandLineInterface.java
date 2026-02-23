package org.banew.report.generation.cli;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.banew.report.generation.services.BasicUsageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Основний командний інтерфейс для генерації одиничних звітів.
 * Забезпечує валідацію вхідних параметрів та запуск процесу побудови документа.
 */
@CommandLine.Command(
        description = "Побудувати фінальний DOCX на основі наданого MD-файлу",
        subcommands = CascadeCommandLineInterface.class
)
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BasicCommandLineInterface implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BasicCommandLineInterface.class);

    @CommandLine.Option(names = {"-ctx", "--contextPath"},
            description = "Тека, що задає контекст для побудови звітів")
    private Path contextPath = Paths.get(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-o", "--outputPath"},
            description = "Назва вихідного файлу (без розширення!)")
    private File outputPath = contextPath.resolve("report").toFile();

    @CommandLine.Option(names = {"-i", "--inputReportMd"},
            description = "Назва вхідного звіту MD")
    private void setInputReportMd(File inputReportMd) throws IOException {
        log.debug("Validating input Markdown file: {}", inputReportMd);

        if (!inputReportMd.exists()) {
            log.error("Input file not found: {}", inputReportMd);
            throw new CommandLine.ParameterException(
                    new CommandLine(this), "Input file " + inputReportMd + " does not exist.");
        }

        log.debug("Checking file header and extension for Markdown compatibility.");
        String text = Files.readString(inputReportMd.toPath()).trim();
        if (!text.startsWith("---") || !inputReportMd.getName().endsWith(".md")) {
            log.error("Invalid file format. File {} lacks YAML front matter or correct extension.", inputReportMd.getName());
            throw new CommandLine.ParameterException(
                    new CommandLine(this), "File " + inputReportMd + " is not a valid Markdown report (missing front matter).");
        }

        this.inputReportMd = inputReportMd;
        log.info("Markdown source file successfully verified.");
    }

    private File inputReportMd;

    @CommandLine.Option(names = {"-t", "--template"},
            description = "Назва template-файлу DOCX")
    private Path templateFile;

    @CommandLine.Option(names = {"-pdf"}, description = "Генерувати PDF")
    private boolean isPdfGenerate;

    @CommandLine.Option(names = {"-docx"}, description = "Генерувати DOCX")
    private boolean isDocxGenerate;

    private final BasicUsageFacade basicUsageFacade;

    @Override
    public void run() {
        log.debug("Starting main execution thread for report generation.");
        log.info("Report processing initiated. Welcome.");

        try {
            if (inputReportMd == null) {
                log.debug("No input file specified. Attempting to locate default 'rom.md' in context: {}", contextPath);
                setInputReportMd(contextPath.resolve("rom.md").toFile());
            }

            if (templateFile == null) {
                log.debug("External template not provided. Falling back to embedded resource.");
                log.info("Using internal default template for document generation.");
            }

            try (InputStream template = templateFile == null ?
                    getClass().getResourceAsStream("/template.docx")
                    : new FileInputStream(templateFile.toFile())) {

                URI romSource = inputReportMd.toURI();
                log.debug("Resolved ROM source URI: {}", romSource);

                log.info("Deserializing content and constructing Report Object Model.");
                basicUsageFacade.process(romSource,
                        contextPath,
                        outputPath,
                        template,
                        isDocxGenerate,
                        isPdfGenerate);

                log.info("Report generation successfully completed. Artifacts saved in: {}", outputPath.getParent());
            }
        } catch (Exception e) {
            log.error("Critical failure during execution: {}", e.getMessage());
            log.info("An unexpected error occurred during processing. Please review the logs.");
            log.error("Stack trace for debugging:", e);
        }
    }
}