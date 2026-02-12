package org.banew.report.generation.projections.builders;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.ImageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class TextContainingPhotoBuilder extends PhotoBuilder {

    private static final Logger log = LoggerFactory.getLogger(TextContainingPhotoBuilder.class);
    private String slice;

    @Override
    public final File build(Path contextPath) throws IOException {
        log.debug("Блядь, стартуєм білд картинки з якимось текстом. Контекст: {}", contextPath);

        log.debug("Визиваєм абстрактну хуйню, хай родить нам текстовий файл");
        File textFile = buildTextFile(contextPath);

        if (slice != null) {
            log.debug("Опа, юзер хоче відрізати кусок м'яса: '{}'. Щас будем шинкувати", slice);
            textFile = generateSlicedFile(textFile);
        } else {
            log.debug("Слайса нема, хаваєм весь файл цілком, як голодні пси");
        }

        try {
            log.debug("Пхаєм цей обрубок у ImageGenerator, хай малює шедевр, сука");
            File generatedPhoto = ImageGenerator.generateCodeImage(textFile.getAbsolutePath());

            if (slice != null) {
                log.debug("Ми робили врємєнний обрубок для слайса, пора його прикопати");
                textFile.delete();
            }

            log.debug("Всьо, фотка готова, забирай цю залупу");
            return generatedPhoto;

        } catch (Exception e) {
            log.debug("Пізда рулю, генератор фоток обригався: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    protected abstract File buildTextFile(Path contextPath) throws IOException;

    private File generateSlicedFile(File textFile) throws IOException {
        log.debug("Читаєм контент файла '{}', шоб порізать його нахуй", textFile.getName());
        String content = Files.readString(textFile.toPath());

        File newTempFile = new File("Рядки [" + slice + "] - " + textFile.getName());

        log.debug("Ріжем рядки по схемі '{}' і ліпим новий файл", slice);
        Files.writeString(newTempFile.toPath(), sliceLines(content, slice));

        return newTempFile;
    }

    private String sliceLines(String content, String range) {
        log.debug("Роздупляєм діапазон слайса: '{}'", range);
        if (range == null || !range.contains("..")) {
            log.debug("Якась хуйня вказана, а не діапазон. Вертаєм всьо як було");
            return content;
        }

        log.debug("Ділим контент по рядках, сука");
        String[] lines = content.split("\\r?\\n");
        String[] parts = range.split("\\.\\.", -1);

        int start = 0;
        int end = lines.length;

        try {
            if (!parts[0].isEmpty()) {
                start = Math.max(0, Integer.parseInt(parts[0].trim()) - 1);
                log.debug("Стартуєм з рядка №{}", start + 1);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Math.min(lines.length, Integer.parseInt(parts[1].trim()));
                log.debug("Кінчаєм на рядку №{}", end);
            }
        } catch (NumberFormatException e) {
            log.debug("Юзер — довбойоб, ввів букви замість цифр. Плюєм на слайс");
            return content;
        }

        if (start >= end || start >= lines.length) {
            log.debug("Діапазон — повна хуйня, старт за кінцем або за файлом. Вертаєм пустоту");
            return "";
        }

        log.debug("Склеюєм назад відібрані {} рядків", end - start);
        return String.join("\n", Arrays.copyOfRange(lines, start, end));
    }
}