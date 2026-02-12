package org.banew.report.generation;

import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.document.SyntaxKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.banew.report.generation.projections.FilePhotoBuilder;
import org.banew.report.generation.projections.ReportObjectModel;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportBuilder {

    public static File generate(ReportObjectModel model, String outputName) throws Exception {

        try (InputStream stream = ReportBuilder.class.getResourceAsStream("/template.docx");
            FileOutputStream out = new FileOutputStream(outputName)) {
            byte[] data = Objects.requireNonNull(stream).readAllBytes();
            data = loadCorrectField(data);
            data = loadTemplateChanges(data, model);
            data = loadImages(data, model);
            out.write(data);
        }

        return new File(outputName);
    }

    private static byte[] loadImages(byte[] data, ReportObjectModel model) throws Exception {
        XWPFDocument doc;
        try (InputStream is = new ByteArrayInputStream(data)) {
            doc = new XWPFDocument(is);
        }

        // Проходимо по всіх параграфах документа
        // Важливо: ми використовуємо копію списку, щоб уникнути ConcurrentModificationException
        List<XWPFParagraph> paragraphs = new ArrayList<>(doc.getParagraphs());
        int imageIndex = 1;

        for (XWPFParagraph p : paragraphs) {
            String text = p.getText();

            // 2. Обробка фоток (Шукаємо {{name}} з твоєї моделі)
            for (Map.Entry<String, FilePhotoBuilder> entry : model.getPhotos().getFiles().entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";

                if (text.contains(placeholder)) {
                    FilePhotoBuilder builder = entry.getValue();
                    File imageFile = builder.build();

                    if (imageFile != null && imageFile.exists()) {
                        p.setAlignment(ParagraphAlignment.CENTER);
                        // ЗАМІСТЬ ПОВНОГО ВИДАЛЕННЯ:
                        // Проходимо по "рунах" (шматках тексту) і міняємо тільки плейсхолдер
                        for (XWPFRun run : p.getRuns()) {
                            String runText = run.getText(0);
                            if (runText != null && runText.contains(placeholder)) {
                                // Міняємо {{penis}} на порожнечу в цій конкретній руні
                                run.setText(runText.replace(placeholder, ""), 0);
                                // Вставляємо картинку в ЦЮ Ж руну (або нову поруч)
                                try (FileInputStream is = new FileInputStream(imageFile)) {
                                    run.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG,
                                            imageFile.getName(), Units.toEMU(350),
                                            Units.toEMU(computeImageHeightByWidth(imageFile, 350)));
                                    run.addBreak();
                                    run.setText("Рис. " + imageIndex + " - " + builder.getLabel());
                                    run.addBreak();
                                }
                            }
                        }
                        imageFile.delete();
                    }
                }
            }
        }

        // Жорстка уніфікація стилів для всього документа
        for (XWPFParagraph p : doc.getParagraphs()) {

            // 2. Налаштування абзацу (вирівнювання та інтервали)
            p.setSpacingBetween(1.5); // Міжрядковий інтервал 1.5

            for (XWPFRun run : p.getRuns()) {
                // 3. Жорстко задаємо шрифт
                run.setFontFamily("Times New Roman");
                run.setColor("000000");
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    private static int computeImageHeightByWidth(File file, int targetWidth) throws Exception {
        try (FileInputStream is = new FileInputStream(file)) {
            BufferedImage bimg = ImageIO.read(is);
            int width = bimg.getWidth();
            int height = bimg.getHeight();

            return  Integer.min((int) ( (double) height / (double) width * targetWidth ), 500);
        }
    }

    private static byte[] loadTemplateChanges(byte[] data, ReportObjectModel model) throws IOException, XDocReportException {
        try (InputStream templateStream = new ByteArrayInputStream(data)) {

            IXDocReport report = XDocReportRegistry.getRegistry()
                    .loadReport(templateStream, TemplateEngineKind.Velocity);

            report.setCacheOriginalDocument(true);

            var metadata = report.createFieldsMetadata();
            metadata.addFieldAsTextStyling("content", SyntaxKind.Html);

            IContext context = report.createContext();

            context.put("content", model.getContent());
            model.getProperties().forEach(context::put);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            report.process(context, outputStream);
            return outputStream.toByteArray();

        }
    }

    private static byte[] loadCorrectField(byte[] data) throws IOException {
        XWPFDocument doc;
        try (InputStream is = new ByteArrayInputStream(data)) {
            doc = new XWPFDocument(Objects.requireNonNull(is));
        }

        // 2. Шукаємо наш текст і міняємо його на справжнє поле
        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = p.getText();
            if (text.contains("${content}")) {
                // Очищаємо параграф
                for (int i = p.getRuns().size() - 1; i >= 0; i--) p.removeRun(i);

                // Вставляємо MERGEFIELD через XML-курсор
                XmlCursor cursor = p.getCTP().newCursor();
                cursor.toEndToken();
                CTSimpleField field = p.getCTP().addNewFldSimple();
                field.setInstr(" MERGEFIELD content ");
                field.addNewR().addNewT().setStringValue("«content»");
                cursor.close();
            }
        }

        // 3. Тепер передаємо цей "підправлений" документ у XDocReport
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);

        return out.toByteArray();
    }
}
