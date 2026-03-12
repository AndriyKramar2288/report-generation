package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервіс для виконання команд у єдиній сесії командного рядка Windows (cmd.exe).
 * <p>
 * Забезпечує інтерактивну взаємодію з процесом, дозволяючи виконувати послідовність
 * команд, передавати вхідні дані (stdin) та перехоплювати вихідний потік (stdout/stderr).
 * Використовує систему маркерів для синхронізації між окремими запусками в межах однієї сесії.
 * </p>
 *
 * @author banew
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Singleton
public class ShellInteractiveRunner {

    private static final Logger log = LoggerFactory.getLogger(ShellInteractiveRunner.class);
    private static final int TERMINATOR_LIVE_TIME_MILLIS = 3000;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /**
     * Запускає сесію cmd.exe та послідовно виконує список команд.
     * <p>
     * Метод підтримує передачу даних користувача в процеси, що очікують вводу.
     * Для розмежування виводу різних команд використовується унікальний текстовий маркер.
     * </p>
     *
     * @param context Робоча директорія, в якій буде запущено командний рядок.
     * @param runs    Список об'єктів {@link BashRun}, що містять команди та дані для вводу.
     * @param hide    Якщо {@code true}, запускає фоновий потік для приховування вікон
     * дочірніх процесів (наприклад, вікна Python/Matplotlib).
     * @return Повний лог консолі за всю сесію у вигляді рядка.
     * @throws IOException Якщо виникла помилка при запуску процесу або роботі з потоками I/O.
     */
    public String runAllInOneSession(Path context, List<? extends BashRun> runs, boolean hide) throws IOException {
        log.debug("Initializing shell session. Operating System - Windows: {}", IS_WINDOWS);
        String shellCmd = IS_WINDOWS ? "powershell.exe" : "zsh";
        ProcessBuilder pb = new ProcessBuilder(shellCmd);
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("LANG", "en_US.UTF-8");
        pb.directory(context.toFile());
        pb.redirectErrorStream(true);

        Process shell = pb.start();
        try {
            StringBuilder finalLog = new StringBuilder();

            Thread terminator = null;
            if (hide && IS_WINDOWS) {
                log.debug("Enabling background window management thread (Terminator).");
                terminator = new Terminator(shell);
                terminator.start();
            }

            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(shell.getOutputStream()));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(shell.getInputStream()));

            String uniqueMarker = "COMMAND_FINISHED_MARKER";
            String line;

            for (BashRun run : runs) {
                log.debug("Executing command: '{}', with provided input: '{}'", run.getCommand(), run.getInput());
                writer.write(run.getCommand() + "\n");
                writer.flush();

                if (run.getInput() != null && !run.getInput().isEmpty()) {
                    log.debug("Handling interactive input from user.");
                    do {
                        char letter = (char) reader.read();
                        finalLog.append(letter);
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            log.error("Input processing sleep interrupted.");
                            throw new RuntimeException(e);
                        }
                    } while (reader.ready());

                    log.debug("Writing interactive input to process stream.");
                    finalLog.append(run.getInput()).append("\n");
                    writer.write(run.getInput() + "\n");
                    writer.flush();
                }

                log.debug("Injecting command completion marker.");
                String errorLevelCmd = IS_WINDOWS ? "echo ERROR_LEVEL:$LASTEXITCODE" : "echo ERROR_LEVEL:$?";
                writer.write(errorLevelCmd + "\n");
                writer.write("echo " + uniqueMarker + "\n");
                writer.flush();

                while (true) {
                    while (run.getInput() != null && !finalLog.toString().contains(run.getInput())) {
                        if (reader.ready()) {
                            finalLog.append((char) reader.read());
                        }
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            log.error("Output synchronization sleep interrupted.");
                            throw new RuntimeException(e);
                        }
                    }

                    line = reader.readLine();
                    if (line == null) break;

                    log.debug("Read line from shell: '{}'", line);

                    if (line.contains("echo " + uniqueMarker) || line.contains("echo ERROR_LEVEL:")) {
                        continue;
                    }

                    if (line.contains("ERROR_LEVEL:")) {
                        String codeStr = line.substring(line.indexOf("ERROR_LEVEL:") + 12).trim();
                        try {
                            int exitCode = Integer.parseInt(codeStr);
                            if (exitCode != 0) {
                                log.error("Command '{}' failed with exit code: {}", run.getCommand(), exitCode);
                                log.error("Execution log dump:\n{}", finalLog);
                                throw new RuntimeException("Process pipeline failure. Exit code: " + exitCode);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse exit code from line: {}", codeStr);
                        }
                        break;
                    }

                    if (line.contains(uniqueMarker)) {
                        log.debug("Completion marker reached. Trimming output.");
                        String before = line.substring(0, line.indexOf(uniqueMarker)).trim();
                        if (!before.isEmpty()) {
                            finalLog.append(before).append("\n");
                        }
                        break;
                    }

                    if (!line.trim().isEmpty()) {
                        finalLog.append(line).append("\n");
                    }
                }
            }

            log.debug("Terminating shell session.");
            writer.write("exit\n");
            writer.flush();

            while ((line = reader.readLine()) != null) {
                if (line.contains("echo " + uniqueMarker)
                        || line.contains(uniqueMarker)
                        || line.contains("echo ERROR_LEVEL:")
                        || line.contains("ERROR_LEVEL:")
                        || line.contains("exit")
                        || line.trim().isEmpty()) {
                    continue;
                }
                finalLog.append(line).append("\n");
            }

            try {
                log.debug("Waiting for shell process to exit.");
                shell.waitFor();
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for process termination.");
                throw new RuntimeException(e);
            }

            if (terminator != null) {
                terminator.interrupt();
            }

            return finalLog.toString().trim();
        } finally {
            if (shell.isAlive()) {
                log.debug("Forcing process destruction.");
                shell.destroyForcibly();
            }
        }
    }

    /**
     * Інтерфейс, що описує одиницю запуску в межах сесії шелла.
     */
    public interface BashRun {
        /**
         * @return Команда для виконання (наприклад, "python script.py").
         */
        String getCommand();

        /**
         * @return Рядок, який буде передано в стандартний ввід процесу.
         */
        String getInput();
    }

    /**
     * Внутрішній потік-демон, призначений для автоматичного приховування вікон
     * дочірніх процесів, запущених через cmd.exe.
     * <p>
     * Використовує JNA для взаємодії з Windows API (User32).
     * </p>
     */
    private static class Terminator extends Thread {

        private final Map<WinDef.HWND, Long> victimsRegistry = new HashMap<>();
        private final Process shell;

        /**
         * Створює нового "Термінатора" для моніторингу конкретного процесу.
         *
         * @param shell Батьківський процес командного рядка.
         */
        public Terminator(Process shell) {
            this.shell = shell;
            setDaemon(true);
        }

        /**
         * Виконує пошук вікон, що належать конкретному PID, та надсилає їм
         * сигнал закриття (WM_CLOSE).
         *
         * @param targetPid PID процесу, чиї вікна потрібно приховати/закрити.
         */
        private void terminateOnlyMyWindows(long targetPid) {
            User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
                IntByReference windowPid = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);

                if (windowPid.getValue() == (int) targetPid) {
                    long currentTime = System.currentTimeMillis();
                    victimsRegistry.putIfAbsent(hwnd, currentTime);

                    long birthTime = victimsRegistry.get(hwnd);
                    long age = currentTime - birthTime;

                    if (age > TERMINATOR_LIVE_TIME_MILLIS) {
                        log.debug("Window for PID {} exceeded lifetime ({} ms). Closing window.", targetPid, age);
                        User32.INSTANCE.PostMessage(hwnd, 0x0112, new WinDef.WPARAM(0xF060), new WinDef.LPARAM(0));
                        victimsRegistry.remove(hwnd);
                    } else {
                        log.debug("Window for PID {} is still within grace period ({} ms).", targetPid, age);
                    }
                }
                return true;
            }, null);
        }

        @Override
        public void run() {
            log.debug("Window management thread started.");
            try {
                while (!Thread.currentThread().isInterrupted() && shell.isAlive()) {
                    shell.descendants().forEach(p -> terminateOnlyMyWindows(p.pid()));
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {
                log.debug("Window management thread interrupted.");
            } finally {
                victimsRegistry.clear();
            }
        }
    }
}