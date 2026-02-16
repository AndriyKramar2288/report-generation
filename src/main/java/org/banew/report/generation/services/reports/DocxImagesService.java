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
        log.debug("Загружаєм картинки. Готовте пам'ять, щас буде боляче");
        XWPFDocument doc;
        try (InputStream is = new ByteArrayInputStream(data)) {
            doc = new XWPFDocument(is);
        }

        Map<String, File> builtImages = images.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, b -> {
                    try {
                        return b.getValue().build(contextPath, toolsSource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));

        List<XWPFParagraph> paragraphs = new ArrayList<>(doc.getParagraphs());
        int imageIndex = 0;

        for (XWPFParagraph p : paragraphs) {
            for (Map.Entry<String, File> entry : builtImages.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                log.debug("Шукаєм плейсхолдер '{}', шоб всунути туди результат", placeholder);

                if (entry.getValue() != null && entry.getValue().exists()) {
                    log.debug("Надибали файл '{}', пхаєм його в параграфи", entry.getValue().getName());
                    if (p.getText().contains(placeholder)) {
                        log.debug("О, попався, сука! Центруєм цей параграф і міняєм текст на фотку");
                        p.setAlignment(ParagraphAlignment.CENTER);

                        for (XWPFRun run : p.getRuns()) {
                            String runText = run.getText(0);
                            if (runText != null && runText.contains(placeholder)) {
                                run.setText(runText.replace(placeholder, ""), 0);

                                try (FileInputStream is = new FileInputStream(entry.getValue())) {
                                    log.debug("Вставляєм картинку і підписуєм, шо це рис. {}", imageIndex + 1);
                                    run.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG,
                                            entry.getValue().getName(), Units.toEMU(350),
                                            Units.toEMU(computeImageHeightByWidth(entry.getValue(), 350)));
                                    run.addBreak();
                                    run.setText("Рис. " + ++imageIndex + " " + images.get(entry.getKey()).getLabel());
                                    run.addBreak();
                                }
                            }
                        }
                    }

                } else {
                    log.debug("Фотки для '{}' нема, скіпаєм цю пусту залупу", placeholder);
                }
            }
        }

        builtImages.values().forEach(File::delete);

        log.debug("Причісуєм стилі, шоб всьо було по ГОСТу, блядь. Таймс нью роман, всі діла");
        for (XWPFParagraph p : doc.getParagraphs()) {
            if (p.getRuns().isEmpty() || p.getText().trim().isEmpty()) {
                log.debug("Параграф пустий, пхаєм туди невидимий пробіл, шоб не схлопнувся");
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
        log.debug("Рахуєм висоту картинки, шоб вона не виглядала як розплющена сперма");
        try (FileInputStream is = new FileInputStream(file)) {
            BufferedImage bimg = ImageIO.read(is);
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            return Integer.min((int) ((double) height / (double) width * targetWidth), 600);
        }
    }
}
