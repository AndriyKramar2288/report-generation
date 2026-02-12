package org.banew.report.generation.projections;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.ImageGenerator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class BashPhotoBuilder extends PhotoBuilder {

    private List<BashRun> runs = new ArrayList<>();

    private String runAllInOneSession(Path context, List<BashRun> runs) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        pb.directory(context.toFile());
        pb.redirectErrorStream(true); // Важливо: зливаємо вивід і помилки в один потік

        Process shell = pb.start();
        StringBuilder finalLog = new StringBuilder();

        // Потоки для спілкування
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(shell.getOutputStream(), "CP866"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream(), "CP866"));

        String endMarker = "COMMAND_FINISHED_MARKER";

        for (BashRun run : runs) {
            // 1. Додаємо імітацію промпта (для краси на фото)
            finalLog.append(context.getFileName()).append("> ").append(run.getCommand()).append("\n");

            // 2. Відправляємо команду + маркер завершення
            writer.write(run.getCommand() + "\n");
            writer.write("echo " + endMarker + "\n"); // Цей рядок з'явиться в reader, коли команда виконається
            writer.flush();

            // 3. Читаємо вивід, поки не зустрінемо наш маркер
            String line;
            while ((line = reader.readLine()) != null) {
                // Ігноруємо відлуння самої команди та наш технічний маркер
                if (line.contains(endMarker)) break;
                if (line.trim().equals(run.getCommand())) continue;

                finalLog.append(line).append("\n");
            }
        }

        // Закриваємо сеанс
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
    public File build(Path contextPath) throws IOException {

        var res = runAllInOneSession(contextPath, runs);
        File runText = Files.createTempFile("run", ".txt").toFile();
        Files.writeString(runText.toPath(), res);

        try {
            File photo = ImageGenerator.generateCodeImage(runText.getAbsolutePath());
            runText.delete();
            return photo;

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class BashRun {
        private String command;
        private String input;
    }
}
