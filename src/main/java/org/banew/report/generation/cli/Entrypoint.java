package org.banew.report.generation.cli;

import org.banew.report.generation.ReportBuilder;
import org.banew.report.generation.projections.ReportObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(
    description = "Побудувати фінальний DOCX на основі наданого MD-файлу"
)
public class Entrypoint implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Entrypoint.class);

    @CommandLine.Option(names = {"-ctx", "--contextPath"},
            description = "Тека, що задає контекст для побудови звітів")
    private Path contextPath = Paths.get(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-o", "--outputPath"},
            description = "Назва вихідного файлу (без розширення!)")
    private File outputPath = contextPath.resolve("report").toFile();

    @CommandLine.Option(names = {"-i", "--inputReportMd"},
            description = "Назва вхідного звіту MD")
    private void setInputReportMd(File inputReportMd) throws IOException {
        if (!inputReportMd.exists()) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this), "Гавнюк, файла " + inputReportMd + " не існує!");
        }

        String text = Files.readString(inputReportMd.toPath()).trim();
        if (!text.startsWith("---") || !inputReportMd.getName().endsWith(".md")) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this), "Гавнюк, файл " + inputReportMd + " не є MD і не має потрібних приколів!");
        }

        this.inputReportMd = inputReportMd;
    }
    private File inputReportMd;

    @CommandLine.Option(names = {"-t", "--template"},
            description = "Назва template-файлу DOCX")
    private File templateFile;

    @CommandLine.Option(names = {"-pdf"}, description = "Генерувати PDF")
    private boolean isPdfGenerate;

    @CommandLine.Option(names = {"-docx"}, description = "Генерувати DOCX")
    private boolean isDocxGenerate;

    @Override
    public void run() {
        try {
            if (inputReportMd == null) {
                setInputReportMd(contextPath.resolve("rom.md").toFile());
            }

            if (templateFile == null) {
                templateFile = Paths.get(getClass().getResource("/template.docx").toURI()).toFile();
            }

            URI romSource = inputReportMd.toURI();

            var rom = ReportObjectModel.create(romSource);
            ReportBuilder.generate(rom,
                    templateFile,
                    outputPath.getAbsolutePath(),
                    contextPath,
                    isDocxGenerate,
                    isPdfGenerate);

            log.debug("Built ROM: " + rom);
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new Entrypoint()).execute(args);
        System.exit(exitCode);
    }
}
