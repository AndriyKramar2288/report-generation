package org.banew.report.generation.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Entrypoint {

    private static final Logger log = LoggerFactory.getLogger(Entrypoint.class);

    public static void main(String[] args) throws Exception {

        try (InputStream is = Entrypoint.class.getResourceAsStream("/logo.shell")) {
            if (is == null) {
                log.warn("Логотип не знайдено, але ми продовжуємо з гідністю.");
            } else {
                String logo = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(logo);
            }
        }

        log.debug("Вказуємо, шо ми тіко на UTF-8 готові думать");
        // 9. Налаштування середовища
        log.info("Налаштування потоків виводу згідно з міжнародними стандартами UTF-8.");
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        log.debug("Точка входу, блядь. Щас Picocli розкидає аргументи як шлюх");
        // 10. Початок розбору аргументів
        log.info("Аналіз параметрів командного рядка...");

        int exitCode = new CommandLine(new BasicCommandLineInterface()).execute(args);

        // 11. Результат виконання
        log.info("Роботу застосунку завершено з кодом стану: {}", exitCode);

        log.debug("Програма закончілась з кодом {}. Валим нахуй!", exitCode);

        // 12. Останнє прощання
        log.info("Дякуємо, що скористалися нашими послугами. Гарного дня!");
        System.exit(exitCode);
    }
}