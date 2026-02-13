package org.banew.report.generation.cli;

import org.banew.report.generation.ReportBuilder;
import org.banew.report.generation.projections.ReportObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@CommandLine.Command(
        description = "Побудувати фінальний DOCX на основі наданого MD-файлу"
)
public class Entrypoint implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Entrypoint.class);

    @CommandLine.Option(names = {"-ctx", "--contextPath"},
            description = "Тека, що задає контекст для побудови звітів")
    private Path contextPath = Paths.get(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-o", "--outputPath"},
            description = "Назва вихідного файлу (без розширення!)")
    private File outputPath = contextPath.resolve("report").toFile();

    @CommandLine.Option(names = {"-i", "--inputReportMd"},
            description = "Назва вхідного звіту MD")
    private void setInputReportMd(File inputReportMd) throws IOException {
        log.debug("Провіряєм, яку хуйню нам підсунули як вхідний MD: {}", inputReportMd);
        if (!inputReportMd.exists()) {
            log.debug("Пізда, файла {} нема, юзер — гавнюк", inputReportMd);
            throw new CommandLine.ParameterException(
                    new CommandLine(this), "Гавнюк, файла " + inputReportMd + " не існує!");
        }

        log.debug("Читаєм перші байти, шоб поняти, чи це реально MD з фронт-маттером");
        String text = Files.readString(inputReportMd.toPath()).trim();
        if (!text.startsWith("---") || !inputReportMd.getName().endsWith(".md")) {
            log.debug("Шо це за параша? {} — це не MD і там нема потрібних приколів!", inputReportMd.getName());
            throw new CommandLine.ParameterException(
                    new CommandLine(this), "Гавнюк, файл " + inputReportMd + " не є MD і не має потрібних приколів!");
        }

        this.inputReportMd = inputReportMd;
        // 1. Вельми культурний лог про успішну валідацію
        log.info("Вихідний матеріал формату Markdown успішно пройшов верифікацію структури.");
    }

    private File inputReportMd;

    @CommandLine.Option(names = {"-t", "--template"},
            description = "Назва template-файлу DOCX")
    private Path templateFile;

    @CommandLine.Option(names = {"-pdf"}, description = "Генерувати PDF")
    private boolean isPdfGenerate;

    @CommandLine.Option(names = {"-docx"}, description = "Генерувати DOCX")
    private boolean isDocxGenerate;

    @Override
    public void run() {
        log.debug("Запускаєм цю шарманку, блядь, в потоці main");
        // 2. Початок роботи
        log.info("Розпочато процес обробки звіту. Ласкаво просимо.");
        try {
            if (inputReportMd == null) {
                log.debug("Юзер провтикав вказати MD, будем шукати rom.md у помойці за шляхом: {}", contextPath);
                setInputReportMd(contextPath.resolve("rom.md").toFile());
            }

            if (templateFile == null) {
                log.debug("Темплейта нема, берем вбудований з ресурсів. Надійся, шо він там лежить, сука");
                // 3. Лог про ресурси
                log.info("Зовнішній шаблон не знайдено. Використовуємо вбудований еталонний зразок.");
            }

            try (InputStream template = templateFile == null ?
                    getClass().getResourceAsStream("/template.docx")
                    : new FileInputStream(templateFile.toFile())) {

                log.debug("Перетворюєм шлях до MD у URI: {}", inputReportMd.toURI());
                URI romSource = inputReportMd.toURI();

                log.debug("Визиваєм магію створення ROM об'єкта");
                // 4. Початок парсингу моделі
                log.info("Здійснюється десериалізація контенту та побудова об'єктної моделі звіту.");
                var rom = ReportObjectModel.create(romSource, contextPath);

                log.debug("Єбать, воно вижило! Ось який ROM ми зліпили: {}", rom);
                // 5. Успіх побудови моделі
                log.info("Об'єктна модель успішно сформована. Кількість знайдених компонентів: {}",
                        (rom.getPhotos().getBash().size() + rom.getCodeFileNameToContentMap().size()));

                log.debug("Запускаєм головний завод по генерації гівна. DOCX: {}, PDF: {}", isDocxGenerate, isPdfGenerate);
                // 6. Фінальний крок
                log.info("Переходимо до стадії фінальної візуалізації та формування вихідних документів.");

                ReportBuilder.generate(Objects.requireNonNull(rom),
                        Objects.requireNonNull(template),
                        outputPath.getAbsolutePath(),
                        contextPath,
                        isDocxGenerate,
                        isPdfGenerate);

                // 7. Тріумфальне завершення
                log.info("Формування звіту завершено успішно. Результати збережено у: {}", outputPath.getParent());
            }
        } catch (Exception e) {
            log.error("Всьо, пізда, приїхали! Помилка: {}", e.getMessage());
            // 8. Культурний опис катастрофи
            log.info("На превеликий жаль, у процесі виконання виникла непередбачувана ситуація. Просимо вибачення за незручності.");
            e.printStackTrace();
            log.error("Якась сперма вилізла в Entrypoint, розбирайся нахуй!");
        }
    }

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

        int exitCode = new CommandLine(new Entrypoint()).execute(args);

        // 11. Результат виконання
        log.info("Роботу застосунку завершено з кодом стану: {}", exitCode);

        log.debug("Програма закончілась з кодом {}. Валим нахуй!", exitCode);

        // 12. Останнє прощання
        log.info("Дякуємо, що скористалися нашими послугами. Гарного дня!");
        System.exit(exitCode);
    }
}