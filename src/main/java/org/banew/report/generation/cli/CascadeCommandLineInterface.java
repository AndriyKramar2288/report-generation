package org.banew.report.generation.cli;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.banew.report.generation.services.CascadeUsageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(
        name = "cascade"
)
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CascadeCommandLineInterface implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CascadeCommandLineInterface.class);

    @CommandLine.Option(names = {"-com", "--CourseObjectModel"},
            description = "Розташування com.xml")
    private File comPath = new File("com.xml");

    @CommandLine.Option(names = {"-b", "--build"}, description = "Почати побудову звітів після створення усіх файлів")
    private boolean isBuild;

    private final CascadeUsageFacade cascadeUsageFacade;

    @Override
    public void run() {
        try {
            if (!comPath.exists() || comPath.isDirectory() || !comPath.canRead() || !comPath.getName().endsWith(".xml")) {
                cascadeUsageFacade.givePrompt();
            } else {
                cascadeUsageFacade.process(comPath, isBuild);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
