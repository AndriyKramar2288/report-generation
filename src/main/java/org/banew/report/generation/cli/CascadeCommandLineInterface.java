package org.banew.report.generation.cli;

import jakarta.xml.bind.JAXBException;
import org.banew.report.generation.cascade.XmlUtils;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(
        name = "cascade"
)
public class CascadeCommandLineInterface implements Runnable {

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

    private void process() throws JAXBException {
        CourseObjectModel cos = XmlUtils.unmashallCourseObjectModel(comPath);

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
