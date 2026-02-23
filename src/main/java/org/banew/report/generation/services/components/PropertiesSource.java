package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class PropertiesSource {

    private static final Logger log = LoggerFactory.getLogger(PropertiesSource.class);

    /**
     * Розташування теки "npm" з встановленими необхідними пакетами (як-от carbon-now)
     */
    @Getter
    private File npmDir;

    @Inject
    public void init() {
        File appLocation = findApplicationLocation();
        log.debug("Application execution context identified at: {}", appLocation.getAbsolutePath());

        npmDir = new File(appLocation, "npm");
        log.info("NPM directory path initialized: {}", npmDir.getAbsolutePath());
    }

    /**
     * @return У разі, якщо програма запускається напряму, повертає корінь проекту. Якщо ж запускається з JAR, то
     * повертається абсолютний шлях теки, в якій розташований JAR.
     */
    private File findApplicationLocation() {
        try {
            log.debug("Resolving application protection domain location...");
            URI locationUri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Paths.get(locationUri);

            if (!codePath.toString().endsWith(".jar")) {
                log.debug("Running in development mode. Using user.dir as root.");
                return new File(System.getProperty("user.dir"));
            }

            log.debug("Running from JAR. Resolving parent directory.");
            return codePath.getParent().toFile();
        }
        catch (Exception e) {
            log.error("Failed to resolve application location", e);
            throw new RuntimeException(e);
        }
    }
}