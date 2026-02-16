package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class ShellRunner {
    public String runAllInOneSession(Path context, List<? extends ShellInteractiveRunner.BashRun> runs) {

    }
}
