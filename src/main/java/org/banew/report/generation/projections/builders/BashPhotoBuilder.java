package org.banew.report.generation.projections.builders;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;
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
    private boolean hide = true;

    private File tempFile;

    private String runAllInOneSession(Path context, List<BashRun> runs) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        pb.directory(context.toFile());
        pb.redirectErrorStream(true);

        Process shell = pb.start();
        StringBuilder finalLog = new StringBuilder();

        Thread terminator = null; // для вбивства невинних віконець, що посміли вилізти
        if (hide) {
            terminator = new Terminator(shell);
            terminator.start();
        }

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(shell.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(shell.getInputStream()));

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

        if (terminator != null) {
            terminator.interrupt();
        }
        return finalLog.toString();
    }


    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        var resultString = runAllInOneSession(contextPath, runs);

        tempFile = Files.createTempFile("run", ".shell").toFile();
        Files.writeString(tempFile.toPath(), resultString);

        return tempFile;
    }

    @Override
    public void close() throws Exception {
        tempFile.delete();
    }

    private static class Terminator extends Thread {
        private final Process shell;

        public Terminator(Process shell) {
            this.shell = shell;
            setDaemon(true);
        }

        private static void terminateOnlyMyWindows(long targetPid) {
            User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
                IntByReference windowPid = new IntByReference();

                // Дізнаємось, який PID створив це вікно
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);

                if (windowPid.getValue() == (int) targetPid) {
                    // Це наше вікно! Закриваємо його
                    User32.INSTANCE.PostMessage(hwnd, 0x0010, null, null); // 0x0010 == WM_CLOSE
                }
                return true;
            }, null);
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && shell.isAlive()) {
                    shell.descendants().forEach(p -> terminateOnlyMyWindows(p.pid()));
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }
    }

    @Data
    public static class BashRun {
        private String command;
        private String input;
    }
}
