package org.banew.report.generation.services.reports;

import fr.opensagres.xdocreport.core.XDocReportException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.banew.report.generation.projections.ReportObjectModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DocxModifierServiceTest {

    private static final String templateExampleWithFields = """
            Текст
            бла-бла
            ${name}
            EEEEEE
            ${content}
            #if(!$codeMap.isEmpty())Код програми
                #foreach($entry in $codeMap.entrySet())
                           ${entry.key}
                ${entry.value}
            #end#end
            """.stripIndent();

    @InjectMocks
    private DocxModifierService docxModifierService;

    @Test
    void resolveFiles_givenDirectoryWithDifferentFiles_successfullyWorks(@TempDir Path tempDir) throws IOException {
        // 1. Готуємо структуру:
        // root/
        //   test1.txt
        //   test2.png
        //   subdir/
        //     test3.txt
        //     empty_dir/  (має бути проігнорована, бо це директорія)

        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Path emptyDir = Files.createDirectory(subDir.resolve("empty_dir"));

        Path file1 = Files.createFile(tempDir.resolve("test1.txt"));
        Path file2 = Files.createFile(tempDir.resolve("test2.png"));
        Path file3 = Files.createFile(subDir.resolve("test3.txt"));

        // 2. Тестуємо пошук усіх .txt файлів (має знайти і в корені, і в subdir)
        // Глоб "**/*.txt" каже: шукай в усіх теках рекурсивно
        List<Path> txtFiles = docxModifierService.resolveFiles(tempDir, "**/*.txt");

        // 3. Ассерти
        assertEquals(2, txtFiles.size(), "Мало знайти рівно 2 текстових файли");
        assertTrue(txtFiles.contains(file1));
        assertTrue(txtFiles.contains(file3));
        assertFalse(txtFiles.contains(file2), "PNG файл не мав потрапити в результат");
        assertFalse(txtFiles.contains(emptyDir), "Директорії мають ігноруватися фільтром !Files.isDirectory");

        // 4. Додаткова перевірка на специфічний файл
        List<Path> singleFile = docxModifierService.resolveFiles(tempDir, "**/test2.png");
        assertEquals(1, singleFile.size());
        assertEquals(file2, singleFile.get(0));
    }

    @Test
    void convertDocxToPdf_givenSmallDocx_containsExpectedText() throws Exception {
        byte[] docxData = generateDocxByString("Banew is here");

        byte[] pdfData = docxModifierService.convertDocxToPdf(docxData);

        try (PDDocument document = PDDocument.load(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertTrue(text.contains("Banew is here"), "Текст із DOCX має бути в PDF");
        }
    }

    @Test
    void loadTemplateChanges_takesValidModel_successFieldsInjects() throws IOException, XDocReportException {
        var docx = generateDocxByString(templateExampleWithFields);
        var model = new ReportObjectModel();
        model.setProperties(Map.of("name", "aboba"));
        model.setContent("CCC");
        model.setCodeFileNameToContentMap(Map.of("File.java", "HELLO WORLD!"));

        byte[] outputDocx = docxModifierService.loadTemplateChanges(docx, model);
        String outputText = getTextFromDocx(outputDocx);
        assertTrue(outputText.contains("aboba"));
        assertTrue(outputText.contains("CCC"));
        assertTrue(outputText.contains("HELLO WORLD!"));
        assertTrue(outputText.contains("File.java"));
    }

    @Test
    void loadTemplateChanges_takesEmptyModel_exceptionsAbsent() throws IOException, XDocReportException {
        var docx = generateDocxByString(templateExampleWithFields);
        var model = new ReportObjectModel();

        byte[] outputDocx = docxModifierService.loadTemplateChanges(docx, model);
    }

    @Test
    void loadCorrectField_contentFieldPresent_successfulMergeFieldAdding() throws IOException {
        var docx = generateDocxByString(templateExampleWithFields);
        byte[] output = docxModifierService.loadCorrectField(docx);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(output))) {

            List<CTSimpleField> fields = document.getParagraphs().stream()
                    .flatMap(p -> Arrays.stream(p.getCTP().getFldSimpleArray()))
                    .toList();

            assertEquals(1, fields.size());

            CTSimpleField field = fields.get(0);
            assertEquals(" MERGEFIELD content ", field.getInstr());
            assertEquals("«content»", field.getRArray(0).getTArray(0).getStringValue());
        }
    }

    private String getTextFromDocx(byte[] docx) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {

            StringBuilder builder = new StringBuilder();

            document.getParagraphs().forEach(paragraph -> {
                paragraph.getRuns().forEach(run -> {
                    builder.append(run.text());
                });
                builder.append("\n");
            });

            return builder.toString();
        }
    }

    private byte[] generateDocxByString(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument(
                Objects.requireNonNull(getClass().getResourceAsStream("/empty.docx")))) {

            for (String s : text.split("\\n")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(s);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}