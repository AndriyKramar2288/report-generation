package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProjectionValidator {

    private static final Logger log = LoggerFactory.getLogger(ProjectionValidator.class);
    private final Validator validator;

    public <T> void validate(T obj) {
        Set<ConstraintViolation<T>> violations = validator.validate(obj);

        if (!violations.isEmpty()) {
            log.error("Упс! Твоя штука — це якась люта діч. Знайдено помилок: {}", violations.size());

            for (ConstraintViolation<T> violation : violations) {
                log.error("Помилка у полі '{}': {}",
                        violation.getPropertyPath(),
                        violation.getMessage());
            }

            throw new RuntimeException("Валідація не пройдена. Виправляй XML!");
        }

        log.info("XML успішно пройшов цензуру. Починаємо працювати!");
    }
}
