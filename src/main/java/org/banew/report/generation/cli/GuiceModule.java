package org.banew.report.generation.cli;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class GuiceModule extends AbstractModule {
    @Provides
    @Singleton
    public Validator provideValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }
}