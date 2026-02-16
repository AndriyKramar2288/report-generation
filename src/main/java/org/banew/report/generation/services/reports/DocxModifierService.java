package org.banew.report.generation.services.reports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.banew.report.generation.projections.ReportObjectModel;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class DocxModifierService {

    private static final Logger log = LoggerFactory.getLogger(DocxModifierService.class);

    /**
     * Виконує пошук файлів у вказаній директорії за заданим шаблоном (glob pattern).
     *
     * @param rootPath Кореневий шлях для пошуку.
     * @param pattern  Шаблон імені файлу (наприклад, "*.png").
     * @return Список знайдених шляхів (Path).
     * @throws IOException При помилках сканування файлової системи.
     */
    public List<Path> resolveFiles(Path rootPath, String pattern) throws IOException {

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(matcher::matches)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Конвертує масив байтів DOCX-документа у PDF-формат.
     * Використовує внутрішній реєстр конвертерів XDocReport.
     *
     * @param data Масив байтів DOCX файлу.
     * @return Масив байтів згенерованого PDF файлу.
     * @throws Exception Якщо конвертер не знайдено або сталася помилка конвертації.
     */
    public byte[] convertDocxToPdf(byte[] data) throws Exception {
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

    /**
     * Виконує підстановку текстових змінних у шаблон через Template Engine (Velocity).
     *
     * @param data  Масив байтів шаблону.
     * @param model Модель даних.
     * @return Документ з підставленими значеннями.
     * @throws IOException          При помилках читання потоків.
     * @throws XDocReportException При помилках обробки Velocity шаблону.
     */
    public byte[] loadTemplateChanges(byte[] data, ReportObjectModel model) throws IOException, XDocReportException {
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

    /**
     * Виконує низькорівневу корекцію полів Word. Замінює текстовий маркер ${content}
     * на об'єкт MERGEFIELD, що дозволяє XDocReport коректно вставляти складний HTML/Rich Text.
     *
     * @param data Масив байтів вхідного документа.
     * @return Масив байтів модифікованого документа.
     * @throws IOException При помилках маніпуляції з XML структурою DOCX.
     */
    public byte[] loadCorrectField(byte[] data) throws IOException {
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
