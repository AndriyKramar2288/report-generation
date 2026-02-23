package org.banew.report.generation.services.reports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.banew.report.generation.projections.builders.PhotoBuilder;
import org.banew.report.generation.services.components.ToolsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class DocxImagesService {

    private static final Logger log = LoggerFactory.getLogger(DocxImagesService.class);
    private final ToolsSource toolsSource;

    /**
     * Вставляє зображення в документ DOCX, замінюючи відповідні плейсхолдери {{key}}.
     * Також виконує фінальну стилізацію документа (шрифти, міжрядкові інтервали).
     *
     * @param data        Масив байтів документа.
     * @param images      Карта будівельників зображень.
     * @param contextPath Шлях для розв'язання відносних шляхів зображень.
     * @return Оновлений масив байтів документа.
     * @throws Exception При помилках маніпуляції з XWPFDocument або відсутності файлів.
     */
    public byte[] loadImages(byte[] data, Map<String, PhotoBuilder> images, Path contextPath) throws Exception {
        log.debug("Initializing image loading process into DOCX document.");
        XWPFDocument doc;
        try (InputStream is = new ByteArrayInputStream(data)) {
            doc = new XWPFDocument(is);
        }

        Map<String, File> builtImages = images.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, b -> {
                    try {
                        return b.getValue().build(contextPath, toolsSource);
                    } catch (IOException e) {
                        log.error("Failed to build image for key: {}", b.getKey());
                        throw new RuntimeException(e);
                    }
                }));

        List<XWPFParagraph> paragraphs = new ArrayList<>(doc.getParagraphs());
        int imageIndex = 0;

        for (XWPFParagraph p : paragraphs) {
            for (Map.Entry<String, File> entry : builtImages.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                log.debug("Searching for placeholder: '{}'", placeholder);

                if (entry.getValue() != null && entry.getValue().exists()) {
                    if (p.getText().contains(placeholder)) {
                        log.debug("Placeholder found. Adjusting paragraph alignment and injecting image: {}", entry.getValue().getName());
                        p.setAlignment(ParagraphAlignment.CENTER);

                        for (XWPFRun run : p.getRuns()) {
                            String runText = run.getText(0);
                            if (runText != null && runText.contains(placeholder)) {
                                run.setText(runText.replace(placeholder, ""), 0);

                                try (FileInputStream is = new FileInputStream(entry.getValue())) {
                                    log.debug("Inserting picture with index: {}", imageIndex + 1);
                                    run.addBreak();
                                    run.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG,
                                            entry.getValue().getName(), Units.toEMU(350),
                                            Units.toEMU(computeImageHeightByWidth(entry.getValue(), 350)));
                                    run.addBreak();
                                    run.setText("Figure " + ++imageIndex + ": " + images.get(entry.getKey()).getLabel());
                                    run.addBreak();
                                }
                            }
                        }
                    }

                } else {
                    log.debug("Image file for placeholder '{}' not found, skipping.", placeholder);
                }
            }
        }

        builtImages.values().forEach(File::delete);

        log.debug("Applying final document styling (fonts, spacing, alignment).");
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (p.getRuns().isEmpty() || p.getText().trim().isEmpty()) {
                XWPFRun r = p.createRun();
                r.setText(" ");
                r.setFontSize(14);
            }
            p.setSpacingBetween(1.5);
            for (XWPFRun run : p.getRuns()) {
                if (!"JetBrains Mono".equals(run.getFontFamily())) {
                    run.setFontFamily("Times New Roman");
                }
                else {
                    p.setSpacingBetween(1);
                }
                run.setColor("000000");
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    /**
     * Розраховує висоту зображення для вставки в документ, зберігаючи пропорції (Aspect Ratio).
     *
     * @param file        Файл зображення.
     * @param targetWidth Цільова ширина в документі (в одиницях POI).
     * @return Розрахована висота (не більше 600 пікселів для уникнення розривів сторінок).
     * @throws Exception Якщо файл не є валідним зображенням.
     */
    private int computeImageHeightByWidth(File file, int targetWidth) throws Exception {
        log.debug("Calculating image aspect ratio for file: {}", file.getName());
        try (FileInputStream is = new FileInputStream(file)) {
            BufferedImage bimg = ImageIO.read(is);
            if (bimg == null) throw new IOException("Failed to read image data.");
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            return Integer.min((int) ((double) height / (double) width * targetWidth), 600);
        }
    }
}