package org.banew.report.generation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class ImageGenerator {

    private static final File npmDir = new File(System.getProperty("user.dir") + "/npm");

    public static File generateCodeImage(String inputPath) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "npx", "carbon-now",
                inputPath,
                "--headless"
        );

        pb.inheritIO();
        pb.directory(npmDir);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            return Objects.requireNonNull(findGeneratedFile());
        }
        else {
            throw new RuntimeException("Process returned non-zero exit code: " + exitCode);
        }
    }

    private static File findGeneratedFile() {
        File[] files = npmDir.listFiles((d, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) return null;

        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) latest = f;
        }
        return latest;
    }
}
