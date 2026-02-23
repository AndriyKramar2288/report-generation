package org.banew.report.generation.cli;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Головна точка входу в застосунок.
 * Відповідає за налаштування середовища, ініціалізацію DI-контейнера та запуск CLI.
 */
public class Entrypoint {

    private static final Logger log = LoggerFactory.getLogger(Entrypoint.class);

    public static void main(String[] args) throws Exception {

        // Відображення логотипу застосунку
        try (InputStream is = Entrypoint.class.getResourceAsStream("/logo.shell")) {
            if (is == null) {
                log.warn("Application logo not found. Proceeding with execution.");
            } else {
                String logo = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(logo);
            }
        }

        log.debug("Configuring system output streams to use UTF-8 encoding.");
        // 9. Налаштування середовища
        log.info("Setting up output streams according to UTF-8 international standards.");
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        log.debug("Initializing Picocli command line parser and Guice injector.");
        // 10. Початок розбору аргументів
        log.info("Analyzing command line parameters...");

        Injector injector = Guice.createInjector(new GuiceModule());
        int exitCode = new CommandLine(BasicCommandLineInterface.class, injector::getInstance).execute(args);

        // 11. Результат виконання
        log.info("Application execution finished with exit code: {}", exitCode);

        log.debug("Process terminated. Final exit code: {}", exitCode);

        // 12. Останнє прощання
        log.info("Thank you for using our services. Have a productive day!");
        System.exit(exitCode);
    }
}