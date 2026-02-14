package org.banew.report.generation.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class PropertiesSource {

    @Getter
    private File npmDir;

    @Inject
    public void init() {
        npmDir = new File(findApplicationLocation(), "npm");
    }

    private File findApplicationLocation() {
        try {
            Path codePath = Paths.get(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            if (!codePath.toString().endsWith(".jar")) {
                return new File(System.getProperty("user.dir"));
            }

            return codePath.getParent().toFile();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
