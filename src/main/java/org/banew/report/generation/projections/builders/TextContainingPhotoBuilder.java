package org.banew.report.generation.projections.builders;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.banew.report.generation.services.components.ToolsSource;
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
    protected ToolsSource toolsSource;

    @Override
    public final File build(Path contextPath, ToolsSource toolsSource) throws IOException {

        this.toolsSource = toolsSource;

        log.debug("Initiating image build process from text content. Context: {}", contextPath);

        log.debug("Invoking abstract method to retrieve source text file.");
        File textFile = buildTextFile(contextPath);

        if (slice != null) {
            log.debug("Slice parameter detected: '{}'. Executing content fragmentation.", slice);
            textFile = generateSlicedFile(textFile);
        } else {
            log.debug("No slice defined. Processing the entire file content.");
        }

        try {
            log.debug("Sending processed text to ImageGenerator for rendering.");
            File generatedPhoto = toolsSource.generateCodeImage(textFile.getAbsolutePath());

            if (slice != null) {
                log.debug("Cleaning up temporary sliced file.");
                textFile.delete();
            }

            log.info("Image generation completed successfully.");
            return generatedPhoto;

        } catch (Exception e) {
            log.error("Image generation failed. Source file: {}, Error: {}", textFile.getName(), e.getMessage());
            throw new RuntimeException("Failed to generate image from text", e);
        }
    }

    protected abstract File buildTextFile(Path contextPath) throws IOException;

    private File generateSlicedFile(File textFile) throws IOException {
        log.debug("Reading file '{}' for content slicing.", textFile.getName());
        String content = Files.readString(textFile.toPath());

        File newTempFile = new File(System.getProperty("java.io.tmpdir"), "sliced_" + System.nanoTime() + "_" + textFile.getName());

        log.debug("Applying line slice scheme '{}' to create a temporary resource.", slice);
        Files.writeString(newTempFile.toPath(), sliceLines(content, slice));

        return newTempFile;
    }

    private String sliceLines(String content, String range) {
        if (range == null || !range.contains("..")) return content;

        String[] lines = content.split("\\r?\\n");
        int total = lines.length;

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
            log.warn("Failed to parse slice range: {}. Returning original content.", range);
            return content;
        }

        start = Math.max(0, Math.min(start, total));
        end = Math.max(0, Math.min(end, total));

        if (start >= end) return "";

        return String.join("\n", Arrays.copyOfRange(lines, start, end));
    }

    /**
     * Допоміжний метод для парсингу "end-5" або просто "10"
     */
    private int parseIndex(String input, int total) {
        String raw = input.toLowerCase().replace(" ", "");

        if (raw.contains("end")) {
            String offsetStr = raw.replace("end", "");
            int offset = offsetStr.isEmpty() ? 0 : Integer.parseInt(offsetStr);
            return total + offset;
        }

        return Integer.parseInt(raw) - 1; // Рахуємо з одиниці для зручності користувача
    }
}