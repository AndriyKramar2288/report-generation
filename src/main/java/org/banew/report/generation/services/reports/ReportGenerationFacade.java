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
     * @param outputName     Базове ім'я вихідного файлу (без розширення).
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

        log.debug("Блядь, стартуєм глобальну хєрню. Тікай з городу, щас буде генерація!");

        Map<String, PhotoBuilder> photos = new LinkedHashMap<>();
        log.debug("Згрібаєм всі фотки в одну кучу: баш, текст, картинки. Якийсь вінегрет, сука");
        photos.putAll(model.getPhotos().getBash());
        photos.putAll(model.getPhotos().getText());
        photos.putAll(model.getPhotos().getImages());

        log.debug("Засмоктуєм шаблон '{}', надійся, шо він не обриганий", template);
        byte[] data = Objects.requireNonNull(template).readAllBytes();

        log.debug("Фіксаєм поля, шоб Ворд не вийожувався");
        data = docxModifierService.loadCorrectField(data);

        log.debug("Пхаєм дані з моделі в шаблон через Velocity. Пливи, плотва!");
        data = docxModifierService.loadTemplateChanges(data, model);

        log.debug("Час засирати документ картинками. Готуй дишіль!");
        data = docxImagesService.loadImages(data, photos, contextPath);

        List<File> outputFiles = new ArrayList<>();

        if (isDocxGenerate) {
            log.debug("Ліпим .docx файл: {}.docx. Хай юзери радуються", outputName);
            File docx = new File(outputName + ".docx");
            outputFiles.add(docx);
            try (FileOutputStream out = new FileOutputStream(docx)) {
                out.write(data);
            }
        }

        if (isPdfGenerate) {
            log.debug("Конвертим цю парашу в PDF, бо солідні люди ворд не читають");
            File pdf = new File(outputName + ".pdf");
            outputFiles.add(pdf);
            try (FileOutputStream out = new FileOutputStream(pdf)) {
                out.write(docxModifierService.convertDocxToPdf(data));
            }
        }

        log.debug("Всьо, блядь, розходимся. Звіт готовий, я спать!");
        return outputFiles;
    }
}