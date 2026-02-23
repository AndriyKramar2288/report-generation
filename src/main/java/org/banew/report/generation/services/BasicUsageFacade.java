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

        log.debug("Starting Report Object Model (ROM) creation from source: {}", romSource);
        var rom = ReportObjectModel.create(romSource, contextPath);

        projectionValidator.validate(rom);
        log.debug("ROM validation successful. Model state: {}", rom);

        // 5. Success of model building
        log.info("Object model successfully formed. Components count: {}",
                (rom.getPhotos().getBash().size() + rom.getCodeFileNameToContentMap().size()));

        log.debug("Initiating document generation process. Target formats - DOCX: {}, PDF: {}", isDocxGenerate, isPdfGenerate);

        // 6. Final step
        log.info("Proceeding to final visualization and output document formation.");

        reportGenerationFacade.generate(Objects.requireNonNull(rom),
                Objects.requireNonNull(template),
                outputPath.getAbsolutePath(),
                contextPath,
                isDocxGenerate,
                isPdfGenerate);
    }
}