package org.banew.report.generation.projections.builders;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.ImageGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class TextContainingPhotoBuilder extends PhotoBuilder {

    private String slice;

    @Override
    public final File build(Path contextPath) throws IOException {

        File textFile = buildTextFile(contextPath);

        textFile = slice == null ? textFile : generateSlicedFile(textFile);

        try {
            File generatedPhoto = ImageGenerator.generateCodeImage(textFile.getAbsolutePath());

            if (slice != null) {
                textFile.delete();
            }

            return generatedPhoto;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract File buildTextFile(Path contextPath) throws IOException;

    private File generateSlicedFile(File textFile) throws IOException {

        String content = Files.readString(textFile.toPath());

        File newTempFile = new File("Рядки [" + slice + "] - " + textFile.getName());
        Files.writeString(newTempFile.toPath(), sliceLines(content, slice));

        return newTempFile;
    }

    private String sliceLines(String content, String range) {
        if (range == null || !range.contains("..")) return content;

        String[] lines = content.split("\\r?\\n");
        String[] parts = range.split("\\.\\.", -1); // -1 важливо для пустих частин

        int start = 0;
        int end = lines.length;

        try {
            // Парсимо старт, якщо він вказаний
            if (!parts[0].isEmpty()) {
                start = Math.max(0, Integer.parseInt(parts[0].trim()) - 1);
            }
            // Парсимо кінець, якщо він вказаний
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Math.min(lines.length, Integer.parseInt(parts[1].trim()));
            }
        } catch (NumberFormatException e) {
            return content; // Якщо юзер ввів херню, вертаємо все як є
        }

        if (start >= end || start >= lines.length) return "";

        return String.join("\n", Arrays.copyOfRange(lines, start, end));
    }
}
