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

    @CommandLine.Option(names = {"-same", "--sameDirectory"}, description = "Будувати всьо прям зразу в одній теці")
    private boolean isSameDirectory;

    private final CascadeUsageFacade cascadeUsageFacade;

    @Override
    public void run() {
        try {
            Path context = comPath.getAbsoluteFile().getParentFile().toPath();
            if (!comPath.exists() || comPath.isDirectory() || !comPath.canRead() || !comPath.getName().endsWith(".xml")) {
                cascadeUsageFacade.givePrompt(context);
            } else {
                cascadeUsageFacade.process(context, isBuild, isSameDirectory);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
