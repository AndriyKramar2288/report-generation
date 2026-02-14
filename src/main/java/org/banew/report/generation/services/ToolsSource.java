package org.banew.report.generation.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class ToolsSource {
    private final ImageGenerator imageGenerator;
    private final ShellRunner shellRunner;

    public File generateCodeImage(String inputPath) throws IOException, InterruptedException {
        return imageGenerator.generateCodeImage(inputPath);
    }

    public String runAllInOneSession(Path context,
                                     List<? extends ShellRunner.BashRun> runs,
                                     boolean hide) throws IOException {
        return shellRunner.runAllInOneSession(context, runs, hide);
    }
}
