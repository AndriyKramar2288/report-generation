package org.banew.report.generation.services.reports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.banew.report.generation.projections.ReportObjectModel;
import org.banew.report.generation.projections.builders.PhotoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;


@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class ReportGenerationFacade {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationFacade.class);
    private final DocxImagesService docxImagesService;
    private final DocxModifierService docxModifierService;

    /**
     * Головний метод генерації звіту. Проводить повний цикл обробки: від завантаження
     * шаблону до збереження вихідних файлів.
     *
     * @param model          Об'єктна модель звіту з даними для підстановки.
     * @param template       Вхідний потік (InputStream) файлу-шаблону .docx.
     * @param outputName     Базове ім'я вихідного файлу (без розширення) в межах контексту.
     * @param contextPath    Шлях до робочої директорії (контексту) для пошуку файлів.
     * @param isDocxGenerate Чи потрібно зберігати результат у форматі DOCX.
     * @param isPdfGenerate  Чи потрібно конвертувати та зберігати результат у форматі PDF.
     * @return Список згенерованих файлів (File).
     * @throws Exception У разі помилок обробки шаблону або проблем з доступом до файлів.
     */
    public List<File> generate(ReportObjectModel model,
                               InputStream template,
                               String outputName,
                               Path contextPath,
                               boolean isDocxGenerate,
                               boolean isPdfGenerate) throws Exception {

        log.debug("Initiating global report generation process.");

        Map<String, PhotoBuilder> photos = new LinkedHashMap<>();
        log.debug("Collecting all photo builders (bash, text, images) into a unified registry.");
        photos.putAll(model.getPhotos().getBash());
        photos.putAll(model.getPhotos().getText());
        photos.putAll(model.getPhotos().getImages());

        log.debug("Reading template stream for output: {}", outputName);
        byte[] data = Objects.requireNonNull(template).readAllBytes();

        log.debug("Performing low-level field correction for MS Word compatibility.");
        data = docxModifierService.loadCorrectField(data);

        log.debug("Injecting model data into template using Velocity engine.");
        data = docxModifierService.loadTemplateChanges(data, model);

        log.debug("Processing and embedding images into the document.");
        data = docxImagesService.loadImages(data, photos, contextPath);

        List<File> outputFiles = new ArrayList<>();

        if (isDocxGenerate) {
            log.debug("Saving generated DOCX file: {}.docx", outputName);
            File docx = contextPath.resolve(outputName + ".docx").toFile();
            outputFiles.add(docx);
            try (FileOutputStream out = new FileOutputStream(docx)) {
                out.write(data);
            }
        }

        if (isPdfGenerate) {
            log.debug("Converting document to PDF format: {}.pdf", outputName);
            File pdf = contextPath.resolve(outputName + ".pdf").toFile();
            outputFiles.add(pdf);
            try (FileOutputStream out = new FileOutputStream(pdf)) {
                out.write(docxModifierService.convertDocxToPdf(data));
            }
        }

        log.info("Report generation successfully completed for: {}", outputName);
        return outputFiles;
    }
}