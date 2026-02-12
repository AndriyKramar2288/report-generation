package org.banew.report.generation.projections.builders;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.ImageGenerator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BashPhotoBuilder extends TextContainingPhotoBuilder {

    private List<BashRun> runs = new ArrayList<>();
    private File tempFile;

    private String runAllInOneSession(Path context, List<BashRun> runs) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        pb.directory(context.toFile());
        pb.redirectErrorStream(true);

        Process shell = pb.start();
        StringBuilder finalLog = new StringBuilder();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(shell.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream(), StandardCharsets.UTF_8));

//        // 2. ВАЖЛИВО: Кажемо консолі працювати в UTF-8 (65001)
//        writer.write("chcp 65001\n");
//        writer.flush();
//
//        // Пропускаємо вивід команди chcp (щоб він не потрапив у фінальний лог)
//        String line;
//        while ((line = reader.readLine()) != null) {
//            if (line.contains("65001")) {
//                reader.readLine();
//                break;
//            }
//        }

        String endMarker = "COMMAND_FINISHED_MARKER";
        String line;

        for (BashRun run : runs) {

            writer.write(run.getCommand() + "\n");

            // 3. Відправляємо маркер завершення
            writer.write("echo " + endMarker + "\n");
            writer.flush();

            // 4. Читаємо результат
            while ((line = reader.readLine()) != null) {
                if (line.contains(endMarker)) break;

                // Ігноруємо "відлуння" (echo) самої команди, щоб не дублювати
                if (line.trim().equals(run.getCommand())) continue;
                if (line.trim().contains("echo " + endMarker)) continue;

                finalLog.append(line).append("\n");
            }
        }

        writer.write("exit\n");
        writer.flush();

        try {
            shell.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return finalLog.toString();
    }


    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        var resultString = runAllInOneSession(contextPath, runs);

        tempFile = Files.createTempFile("run", ".shell").toFile();
        Files.writeString(tempFile.toPath(), resultString, StandardCharsets.UTF_8);

        return tempFile;
    }

    @Override
    public void close() throws Exception {
        tempFile.delete();
    }

    @Data
    public static class BashRun {
        private String command;
        private String input;
    }
}
