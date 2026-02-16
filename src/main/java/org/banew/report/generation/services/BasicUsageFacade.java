package org.banew.report.generation.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.banew.report.generation.projections.ReportObjectModel;
import org.banew.report.generation.services.components.ProjectionValidator;
import org.banew.report.generation.services.reports.ReportGenerationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BasicUsageFacade {

    private static final Logger log = LoggerFactory.getLogger(BasicUsageFacade.class);
    private final ProjectionValidator projectionValidator;
    private final ReportGenerationFacade reportGenerationFacade;

    public void process(URI romSource,
                        Path contextPath,
                        File outputPath,
                        InputStream template,
                        boolean isDocxGenerate,
                        boolean isPdfGenerate) throws Exception {
        var rom = ReportObjectModel.create(romSource, contextPath);
        projectionValidator.validate(rom);
        log.debug("Єбать, воно вижило! Ось який ROM ми зліпили: {}", rom);
        // 5. Успіх побудови моделі
        log.info("Об'єктна модель успішно сформована. Кількість знайдених компонентів: {}",
                (rom.getPhotos().getBash().size() + rom.getCodeFileNameToContentMap().size()));

        log.debug("Запускаєм головний завод по генерації гівна. DOCX: {}, PDF: {}", isDocxGenerate, isPdfGenerate);
        // 6. Фінальний крок
        log.info("Переходимо до стадії фінальної візуалізації та формування вихідних документів.");

        reportGenerationFacade.generate(Objects.requireNonNull(rom),
                Objects.requireNonNull(template),
                outputPath.getAbsolutePath(),
                contextPath,
                isDocxGenerate,
                isPdfGenerate);
    }
}
