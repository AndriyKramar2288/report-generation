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
        if (range == null || !range.contains("..")) return content;

        String[] lines = content.split("\\r?\\n");
        int total = lines.length; // Наше "end"

        String[] parts = range.split("\\.\\.", -1);

        int start = 0;
        int end = total;

        try {
            if (!parts[0].isEmpty()) {
                start = parseIndex(parts[0].trim(), total);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = parseIndex(parts[1].trim(), total);
            }
        } catch (Exception e) {
            return content; // Якщо юзер перемудрив з математикою
        }
        
        start = Math.max(0, Math.min(start, total));
        end = Math.max(0, Math.min(end, total));

        if (start >= end) return "";

        return String.join("\n", Arrays.copyOfRange(lines, start, end));
    }

    // Допоміжний метод для парсингу "end-5" або просто "10"
    private int parseIndex(String input, int total) {
        String raw = input.toLowerCase().replace(" ", "");

        if (raw.contains("end")) {
            // Обробляємо "end-5" або "end+2" (хоча +2 це дивно, але хай буде)
            String offsetStr = raw.replace("end", "");
            int offset = offsetStr.isEmpty() ? 0 : Integer.parseInt(offsetStr);
            return total + offset;
        }

        // Стара логіка для звичайних чисел
        return Integer.parseInt(raw) - 1; // -1 бо юзер рахує з одиниці
    }
}