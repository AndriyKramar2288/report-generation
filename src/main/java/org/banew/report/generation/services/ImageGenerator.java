package org.banew.report.generation.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Дозволяє генерувати файні фото з вмістом деякого файлу.
 * <b>Thread-Safe</b>
 */
@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class ImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private final PropertiesSource propertiesSource;

    /**
     * Генерує файл зображення з вмістом деякого файлу
     * @param inputPath абсолютний шлях до будь-якого текстового файлу
     * @return вихідне зображення
     * @throws IOException у разі, якщо не буде знайдено фото
     */
    public synchronized File generateCodeImage(String inputPath) throws IOException, InterruptedException {

        log.debug("Так, блядь, готуємся фоткати код. Вхідна залупа: {}", inputPath);
        log.debug("Робоча каморка для npm: {}", propertiesSource.getNpmDir().getAbsolutePath());

        log.debug("Запрягаєм npx carbon-now. Хай ця паскуда малює нам красу через headless-браузер");
        ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "npx", "carbon-now",
                Objects.requireNonNull(inputPath),
                "--headless"
        );

        log.debug("Наслєдуєм IO, шоб бачити в консолі, як ця сука мучиться");
        pb.inheritIO();
        pb.directory(propertiesSource.getNpmDir());

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

    /**
     * Обшукує усю {@code propertiesSource.getNpmDir()} з метою знайти щойно згенероване фото
     * @return згенероване фото
     */
    @Nullable
    private File findGeneratedFile() {
        log.debug("Шманаєм папку '{}' на прєдмет свіжих .png фоток", propertiesSource.getNpmDir().getName());
        File[] files = propertiesSource.getNpmDir().listFiles((d, name) -> name.endsWith(".png"));

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