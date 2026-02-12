package org.banew.report.generation.projections.builders;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.ptr.IntByReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BashPhotoBuilder extends TextContainingPhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(BashPhotoBuilder.class);
    private List<BashRun> runs = new ArrayList<>();
    private boolean hide = true;
    private File tempFile;

    private String runAllInOneSession(Path context, List<BashRun> runs) throws IOException {
        log.debug("Сука, заводим цю колимагу cmd.exe, шоб вона всралась");
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        pb.directory(context.toFile());
        pb.redirectErrorStream(true);

        Process shell = pb.start();
        StringBuilder finalLog = new StringBuilder();

        Thread terminator = null;
        if (hide) {
            log.debug("Визиваєм кіллера-термінатора, хай пиздячить вікна нахуй");
            terminator = new Terminator(shell);
            terminator.start();
        }

        log.debug("Настраюєм потоки, блядь, шоб юнікод не здох як сука");
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(shell.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(shell.getInputStream(), StandardCharsets.UTF_8));

        log.debug("Шиєм цій паскуді chcp 65001, хай понімає мову, падла");
        writer.write("chcp 65001\n");
        writer.flush();

        String uniqueMarker = "COMMAND_FINISHED_MARKER";
        String line;

        log.debug("Ждем, пока ця хуйня прочехлиця після зміни кодіровки");
        while ((line = reader.readLine()) != null) {
            if (line.contains("65001")) {
                log.debug("О, роздуплилась, ковтаєм остальний мусор");
                reader.readLine();
                break;
            }
        }

        for (BashRun run : runs) {
            log.debug("Стартуєм новий запуск: '{}', вхідна хуйня: '{}'", run.getCommand(), run.getInput());
            writer.write(run.getCommand() + "\n");
            writer.flush();

            if (run.getInput() != null && !run.getInput().isEmpty()) {
                log.debug("Опа, є якийсь ввід, щас будем сосати букви з рідера");
                do {
                    log.debug("Смокчем по одній букві, блядь, як шлюхи");
                    finalLog.append((char) reader.read());
                    try {
                        Thread.sleep(50);
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
            writer.write("echo " + uniqueMarker + "\n");
            writer.flush();

            while ((line = reader.readLine()) != null) {
                log.debug("Зчитали строку, блядь: '{}'", line);

                if (line.contains("echo " + uniqueMarker)) {
                    log.debug("Це просто ехо нашого маркера, ігнорим цю залупу");
                    continue;
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
    }

    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        log.debug("Запускаєм сесію, щас буде гарячо");
        var resultString = runAllInOneSession(contextPath, runs);

        log.debug("Ліпим врємєнний файл, шоб запхати туди цей брєд");
        tempFile = Files.createTempFile("run", ".shell").toFile();
        Files.writeString(tempFile.toPath(), resultString, StandardCharsets.UTF_8);

        return tempFile;
    }

    @Override
    public void close() throws Exception {
        log.debug("Убираєм за собою гівно, видаляєм файл");
        if (tempFile != null) tempFile.delete();
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
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);

                if (windowPid.getValue() == (int) targetPid) {
                    log.debug("Надибали вікно підараса з PID {}. ГАСИ ЙОГО!", targetPid);
                    User32.INSTANCE.PostMessage(hwnd, 0x0010, null, null);
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
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BashRun {
        private String command;
        private String input;
    }
}