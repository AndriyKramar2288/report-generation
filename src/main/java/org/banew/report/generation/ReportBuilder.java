package org.banew.report.generation;

import fr.opensagres.xdocreport.converter.ConverterRegistry;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.IConverter;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.document.DocumentKind;
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
import org.banew.report.generation.projections.ReportObjectModel;
import org.banew.report.generation.projections.builders.PhotoBuilder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReportBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReportBuilder.class);

    public static void generate(ReportObjectModel model,
                                InputStream template,
                                String outputName,
                                Path contextPath,
                                boolean isDocxGenerate,
                                boolean isPdfGenerate) throws Exception {

        log.debug("Блядь, стартуєм глобальну хєрню. Тікай з городу, щас буде генерація!");

        Map<String, PhotoBuilder> photos = new LinkedHashMap<>();
        log.debug("Згрібаєм всі фотки в одну кучу: баш, текст, картинки. Якийсь вінегрет, сука");
        photos.putAll(model.getPhotos().getBash());
        photos.putAll(model.getPhotos().getText());
        photos.putAll(model.getPhotos().getImages());

        log.debug("Засмоктуєм шаблон '{}', надійся, шо він не обриганий", template);
        byte[] data = Objects.requireNonNull(template).readAllBytes();

        log.debug("Фіксаєм поля, шоб Ворд не вийожувався");
        data = loadCorrectField(data);

        log.debug("Пхаєм дані з моделі в шаблон через Velocity. Пливи, плотва!");
        data = loadTemplateChanges(data, model);

        log.debug("Час засирати документ картинками. Готуй дишіль!");
        data = loadImages(data, photos, contextPath);

        if (isDocxGenerate) {
            log.debug("Ліпим .docx файл: {}.docx. Хай юзери радуються", outputName);
            try (FileOutputStream out = new FileOutputStream(outputName + ".docx")) {
                out.write(data);
            }
        }

        if (isPdfGenerate) {
            log.debug("Конвертим цю парашу в PDF, бо солідні люди ворд не читають");
            try (FileOutputStream out = new FileOutputStream(outputName + ".pdf")) {
                out.write(convertDocxToPdf(data));
            }
        }

        log.debug("Всьо, блядь, розходимся. Звіт готовий, я спать!");
    }

    private static List<Path> resolveFiles(Path rootPath, String pattern) throws IOException {

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(matcher::matches)
                    .collect(Collectors.toList());
        }
    }

    private static byte[] convertDocxToPdf(byte[] data) throws Exception {
        log.debug("Починаєм магічне перетворення гівна в PDF");
        try (InputStream in = new ByteArrayInputStream(data);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Options options = Options.getFrom(DocumentKind.DOCX).to(ConverterTypeTo.PDF);
            IConverter converter = ConverterRegistry.getRegistry().getConverter(options);

            log.debug("Конвертер каже: 'Я зроблю це, але мені гидко'");
            converter.convert(in, out, options);

            return out.toByteArray();
        }
    }

    private static byte[] loadImages(byte[] data, Map<String, PhotoBuilder> images, Path contextPath) throws Exception {
        log.debug("Загружаєм картинки. Готовте пам'ять, щас буде боляче");
        XWPFDocument doc;
        try (InputStream is = new ByteArrayInputStream(data)) {
            doc = new XWPFDocument(is);
        }

        Map<String, File> builtImages = images.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, b -> {
                    try {
                        return b.getValue().build(contextPath);
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

    private static int computeImageHeightByWidth(File file, int targetWidth) throws Exception {
        log.debug("Рахуєм висоту картинки, шоб вона не виглядала як розплющена сперма");
        try (FileInputStream is = new FileInputStream(file)) {
            BufferedImage bimg = ImageIO.read(is);
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            return Integer.min((int) ((double) height / (double) width * targetWidth), 600);
        }
    }

    private static byte[] loadTemplateChanges(byte[] data, ReportObjectModel model) throws IOException, XDocReportException {
        log.debug("Запускаєм XDocReport і Velocity, хай міняють змінні на реальне гівно");
        try (InputStream templateStream = new ByteArrayInputStream(data)) {
            IXDocReport report = XDocReportRegistry.getRegistry()
                    .loadReport(templateStream, TemplateEngineKind.Velocity);
            report.setCacheOriginalDocument(true);

            var metadata = report.createFieldsMetadata();
            metadata.addFieldAsTextStyling("content", SyntaxKind.Html);

            IContext context = report.createContext();
            context.put("content", model.getContent());
            context.put("codeMap", model.getCodeFileNameToContentMap());
            model.getProperties().forEach((k, v) -> {
                log.debug("Пхаєм в контекст: {} = {}", k, v);
                context.put(k, v);
            });

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            report.process(context, outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] loadCorrectField(byte[] data) throws IOException {
        log.debug("Маніпулюєм XML-курсором, шоб вставити MERGEFIELD. Чиста содомія!");
        XWPFDocument doc;
        try (InputStream is = new ByteArrayInputStream(data)) {
            doc = new XWPFDocument(Objects.requireNonNull(is));
        }

        for (XWPFParagraph p : doc.getParagraphs()) {
            if (p.getText().contains("${content}")) {
                log.debug("Надибали плейсхолдер контента, щас будем його роздирати");
                for (int i = p.getRuns().size() - 1; i >= 0; i--) p.removeRun(i);

                XmlCursor cursor = p.getCTP().newCursor();
                cursor.toEndToken();
                CTSimpleField field = p.getCTP().addNewFldSimple();
                field.setInstr(" MERGEFIELD content ");
                field.addNewR().addNewT().setStringValue("«content»");
                cursor.close();
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }
}