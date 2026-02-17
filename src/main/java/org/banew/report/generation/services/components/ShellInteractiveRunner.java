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
     *                дочірніх процесів (наприклад, вікна Python/Matplotlib).
     * @return Повний лог консолі за всю сесію у вигляді рядка.
     * @throws IOException Якщо виникла помилка при запуску процесу або роботі з потоками I/O.
     */
    public String runAllInOneSession(Path context, List<? extends BashRun> runs, boolean hide) throws IOException {
        log.debug("Сука, заводим цю колимагу cmd.exe, шоб вона всралась");
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("LANG", "en_US.UTF-8");
        pb.directory(context.toFile());
        pb.redirectErrorStream(true);

        Process shell = pb.start();
        try {
            StringBuilder finalLog = new StringBuilder();

            Thread terminator = null;
            if (hide) {
                log.debug("Визиваєм кіллера-термінатора, хай пиздячить вікна нахуй");
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
                log.debug("Стартуєм новий запуск: '{}', вхідна хуйня: '{}'", run.getCommand(), run.getInput());
                writer.write(run.getCommand() + "\n");
                writer.flush();

                if (run.getInput() != null && !run.getInput().isEmpty()) {
                    log.debug("Опа, є якийсь ввід, щас будем сосати букви з рідера");
                    do {
                        char letter = (char) reader.read();
                        finalLog.append(letter);
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            log.debug("Якийсь підарас перебив нам сон, сука");
                            throw new RuntimeException(e);
                        }
                    } while (reader.ready());

                    log.debug("Пхаєм в лог і в прогу цей йобаний ввід");
                    finalLog.append(run.getInput()).append("\n");
                    writer.write(run.getInput() + "\n");
                    writer.flush();
                }

                log.debug("Швиряєм маркер, шоб знати, де ця параша кінчається");
                writer.write("echo ERROR_LEVEL:%ERRORLEVEL%\n");
                writer.write("echo " + uniqueMarker + "\n");
                writer.flush();

                long startWait = System.currentTimeMillis();
                while (true) {
                    while (run.getInput() != null && !finalLog.toString().contains(run.getInput())) {
                        if (reader.ready()) {
                            finalLog.append((char) reader.read());
                        }
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            log.debug("ХТО СУКААА");
                            throw new RuntimeException(e);
                        }
                    }

                    line = reader.readLine();
                    log.debug("Зчитали строку, блядь: '{}'", line);

                    if (line.contains("echo " + uniqueMarker) || line.contains("echo ERROR_LEVEL:")) {
                        log.debug("Це просто ехо нашого маркера, ігнорим цю залупу");
                        continue;
                    }

                    if (line.contains("ERROR_LEVEL:")) {
                        String codeStr = line.substring(line.indexOf("ERROR_LEVEL:") + 12).trim();
                        try {
                            int exitCode = Integer.parseInt(codeStr);
                            if (exitCode != 0) {
                                log.error("Сука, команда '{}' впала з кодом {}!", run.getCommand(), exitCode);
                                log.error("Полотно:\n{}", finalLog.toString());
                                // Тут ти можеш або кинути ексепшн відразу, або помітити статус
                                throw new RuntimeException("Команда впала, пайплайну пізда. Код: " + exitCode);
                            }
                        } catch (NumberFormatException e) {
                            log.debug("Не зміг розпарсити код помилки, якась херня прийшла: {}", codeStr);
                        }
                        break; // Виходимо з циклу читання для цієї команди
                    }

                    if (line.contains(uniqueMarker)) {
                        log.debug("Єбать, надибали маркер! Обрізаєм лишню сперму");
                        String before = line.substring(0, line.indexOf(uniqueMarker)).trim();
                        if (!before.isEmpty()) {
                            log.debug("Дописуєм в лог то, шо було перед маркером: '{}'", before);
                            finalLog.append(before).append("\n");
                        }
                        break;
                    }

                    if (line.trim().isEmpty()) {
                        log.debug("Строка пуста як голова депутата, скіпаєм");
                        continue;
                    }

                    log.debug("Норм тема, пхаєм строку в фінальний лог");
                    finalLog.append(line).append("\n");
                }
            }

            log.debug("Кажем cmd 'exit' і валим нахуй");
            writer.write("exit\n");
            writer.flush();

            log.debug("Доїдаєм залишки логів перед смертю шелла...");
            while ((line = reader.readLine()) != null) {
                log.debug("Прилетіло \"на коня\": '{}'", line);

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
                log.debug("Ждем, пока цей труп шелла остаточно охолоне");
                shell.waitFor();
            } catch (InterruptedException e) {
                log.debug("Блядь, і тут нас перебили, шо за хуйня");
                throw new RuntimeException(e);
            }

            if (terminator != null) {
                log.debug("Вирубаєм термінатора, хай іде курити");
                terminator.interrupt();
            }

            log.debug("Всьо, блядь, готово. Вертаєм цей обриганий лог");
            return finalLog.toString().trim();
        } finally {
            log.debug("Закриваєм лавочку примусово");
            if (shell.isAlive()) {
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

                    if (age > 500) {
                        log.debug("Вікну з PID {} вже {} мс. Час вийшов, бабай прийшов!", targetPid, age);
                        User32.INSTANCE.PostMessage(hwnd, 0x0112, new WinDef.WPARAM(0xF060), new WinDef.LPARAM(0));
                        victimsRegistry.remove(hwnd);
                    } else {
                        log.debug("Вікно PID {} ще молоде ({} мс), хай погуляє", targetPid, age);
                    }
                }
                return true;
            }, null);
        }

        @Override
        public void run() {
            log.debug("Кіллер вийшов на охоту");
            try {
                while (!Thread.currentThread().isInterrupted() && shell.isAlive()) {
                    shell.descendants().forEach(p -> terminateOnlyMyWindows(p.pid()));
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {
                log.debug("Кіллера повязали, він спать");
            } finally {
                victimsRegistry.clear();
            }
        }
    }
}
