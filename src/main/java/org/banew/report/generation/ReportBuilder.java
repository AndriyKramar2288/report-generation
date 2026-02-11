package org.banew.report.generation;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.banew.report.generation.projections.ReportObjectModel;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ReportBuilder {
    public static File generate(ReportObjectModel model) throws Exception {

        Map<String, File> parsedPhotos = new HashMap<>();

        model.getPhotos().getFiles().forEach((name, builder) -> {
            parsedPhotos.put(name, builder.build());
        });

        try (XWPFDocument doc = new XWPFDocument(
                Objects.requireNonNull(ReportBuilder.class.getResourceAsStream("/template.docx")))) {

            fillTemplate(doc, model.getProperties());

            // 1. Створюємо парсер
            Parser parser = Parser.builder().build();
            Node document = parser.parse(model.getContent());

            // 2. Створюємо Visitor, який "малює" у Word
            NodeVisitor visitor = new NodeVisitor(
                    new VisitHandler<>(Heading.class, h -> {
                        XWPFParagraph p = doc.createParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(h.getText().toString());
                        run.setBold(true);
                        run.setFontSize(20 - (h.getLevel() * 2));
                    }),

                    new VisitHandler<>(Paragraph.class, pg -> {
                        String originalText = pg.getContentChars().toString().trim();

                        // 1. ПЕРЕВІРКА НА ФОТО
                        // Використовуємо findFirst, щоб не плодити копії, якщо знайшли збіг
                        Optional<String> photoKey = parsedPhotos.keySet().stream()
                                .filter(name -> originalText.contains("{{" + name + "}}"))
                                .findFirst();

                        if (photoKey.isPresent()) {
                            File file = parsedPhotos.get(photoKey.get());
                            insertImage(doc, file.getAbsolutePath(), 400, 300);
                            return; // Виходимо, щоб не дублювати цей параграф як текст
                        }

                        // 2. ПЕРЕВІРКА НА ЗАМІНУ ТЕКСТУ (Властивості моделі)
                        String processedText = originalText;
                        boolean hasProperty = false;

                        for (var entry : model.getProperties().entrySet()) {
                            String placeholder = "{{" + entry.getKey() + "}}";
                            if (processedText.contains(placeholder)) {
                                processedText = processedText.replace(placeholder, entry.getValue());
                                hasProperty = true;
                            }
                        }

                        // 3. ВИВІД У WORD
                        // Якщо це не фото, створюємо звичайний параграф (з заміною або без)
                        XWPFParagraph p = doc.createParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(processedText);
                    })
            );

            // 3. Запускаємо обхід дерева
            visitor.visit(document);

            // 4. Зберігаємо файл
            try (FileOutputStream out = new FileOutputStream("GeneratedReport.docx")) {
                doc.write(out);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        parsedPhotos.values().forEach(File::delete);

        return new File("GeneratedReport.docx");
    }

    private static void insertImage(XWPFDocument doc, String imgPath, int width, int height) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER); // Центруємо картинку
        XWPFRun run = p.createRun();

        try (InputStream is = new FileInputStream(imgPath)) {
            // Додаємо картинку
            run.addPicture(
                    is,
                    XWPFDocument.PICTURE_TYPE_PNG, // Тип файлу
                    imgPath,                       // Назва
                    Units.toEMU(width),            // Ширина в одиницях EMU
                    Units.toEMU(height)            // Висота в одиницях EMU
            );

            // Додаємо підпис під картинкою (опціонально)
            XWPFParagraph caption = doc.createParagraph();
            caption.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun captionRun = caption.createRun();
            captionRun.setItalic(true);
            captionRun.setText("Рис. — Згенерований фрагмент коду");

        } catch (Exception e) {
            System.err.println("Не вдалося вставити картинку: " + e.getMessage());
        }
    }

    private static void fillTemplate(XWPFDocument doc, Map<String, String> properties) {
        // 1. Обробляємо звичайні параграфи
        for (XWPFParagraph p : doc.getParagraphs()) {
            replaceInParagraph(p, properties);
        }

        // 2. Обробляємо таблиці (якщо в тебе там теж є мітки)
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraph(p, properties);
                    }
                }
            }
        }
    }

    private static void replaceInParagraph(XWPFParagraph p, Map<String, String> properties) {
        List<XWPFRun> runs = p.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // Склеюємо весь текст параграфа в один рядок
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun r : runs) {
            String text = r.getText(0);
            if (text != null) fullText.append(text);
        }

        String content = fullText.toString();
        boolean updated = false;

        // Проходимо по мапі і замінюємо
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (content.contains(placeholder)) {
                content = content.replace(placeholder, entry.getValue());
                updated = true;
            }
        }

        // Якщо щось замінили — перезаписуємо рани
        if (updated) {
            // Видаляємо всі старі рани, крім першого
            for (int i = runs.size() - 1; i > 0; i--) {
                p.removeRun(i);
            }
            // В перший ран записуємо оновлений текст, зберігаючи форматування
            XWPFRun run = runs.get(0);
            run.setText(content, 0);
        }
    }
}
