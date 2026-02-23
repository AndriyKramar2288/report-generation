package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Сервіс для валідації об'єктних моделей звітів та курсів.
 * Використовує Jakarta Validation для перевірки обмежень, заданих анотаціями.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProjectionValidator {

    private static final Logger log = LoggerFactory.getLogger(ProjectionValidator.class);
    private final Validator validator;

    /**
     * Виконує повну валідацію переданого об'єкта.
     * * @param obj Об'єкт для перевірки.
     * @param <T> Тип об'єкта.
     * @throws RuntimeException у разі виявлення порушень обмежень.
     */
    public <T> void validate(T obj) {
        log.debug("Starting object validation for type: {}", obj.getClass().getSimpleName());
        Set<ConstraintViolation<T>> violations = validator.validate(obj);

        if (!violations.isEmpty()) {
            log.error("Validation failed. Found {} constraint violation(s).", violations.size());

            for (ConstraintViolation<T> violation : violations) {
                log.error("Violation in field '{}': {}",
                        violation.getPropertyPath(),
                        violation.getMessage());
            }

            throw new RuntimeException("Validation failed. Please check the input data and structure!");
        }

        log.info("Validation successful. Object model conforms to the required constraints.");
    }
}