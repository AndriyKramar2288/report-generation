package org.banew.report.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private static final File npmDir = new File(findApplicationLocation(), "npm");

    public static File generateCodeImage(String inputPath) throws IOException, InterruptedException {
        log.debug("Так, блядь, готуємся фоткати код. Вхідна залупа: {}", inputPath);
        log.debug("Робоча каморка для npm: {}", npmDir.getAbsolutePath());

        log.debug("Запрягаєм npx carbon-now. Хай ця паскуда малює нам красу через headless-браузер");
        ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "npx", "carbon-now",
                inputPath,
                "--headless"
        );

//        log.debug("Наслєдуєм IO, шоб бачити в консолі, як ця сука мучиться");
//        pb.inheritIO();
        pb.directory(npmDir);

        log.debug("Стартуєм процес. Тікай з городу, щас Carbon почне жерати оперативу!");
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            log.debug("Заєбісь! Процес вижив і не обригався. Шукаєм, де ця падла кинула файл");
            return Objects.requireNonNull(findGeneratedFile());
        }
        else {
            log.debug("Пізда рулю! Carbon вернув якийсь лєвий код: {}. Розбирайся сам, чо воно здохло", exitCode);
            throw new RuntimeException("Process returned non-zero exit code: " + exitCode);
        }
    }

    public static File findApplicationLocation() {
        try {
            Path codePath = Paths.get(
                    ImageGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            if (!codePath.toString().endsWith(".jar")) {
                return new File(System.getProperty("user.dir"));
            }

            return codePath.getParent().toFile();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static File findGeneratedFile() {
        log.debug("Шманаєм папку '{}' на прєдмет свіжих .png фоток", npmDir.getName());
        File[] files = npmDir.listFiles((d, name) -> name.endsWith(".png"));

        if (files == null || files.length == 0) {
            log.debug("Ну і де фотка? Обійшов всьо — ніхуя нема. Нас найбали, розходимся!");
            return null;
        }

        log.debug("Надибали {} штук. Щас виберем ту, шо сама свіжа, як пиріжок з лівером", files.length);
        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }

        log.debug("Ось вона, наша лапочка: '{}'. Тягнем її в звіт, поки не протухла", latest.getName());
        return latest;
    }
}