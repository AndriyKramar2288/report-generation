package org.banew.report.generation.services.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ =  @Inject)
@Singleton
public class ToolsSource {
    private final ImageGenerator imageGenerator;
    private final ShellInteractiveRunner shellInteractiveRunner;

    /**
     * Генерує файл зображення з вмістом деякого файлу
     * @param inputPath абсолютний шлях до будь-якого текстового файлу
     * @return вихідне зображення
     * @throws IOException у разі, якщо не буде знайдено фото
     */
    public File generateCodeImage(String inputPath) throws IOException, InterruptedException {
        return imageGenerator.generateCodeImage(inputPath);
    }

    /**
     * Запускає сесію cmd.exe та послідовно виконує список команд.
     * <p>
     * Метод підтримує передачу даних користувача в процеси, що очікують вводу.
     * Для розмежування виводу різних команд використовується унікальний текстовий маркер.
     * </p>
     *
     * @param context Робоча директорія, в якій буде запущено командний рядок.
     * @param runs    Список об'єктів {@link ShellInteractiveRunner.BashRun}, що містять команди та дані для вводу.
     * @param hide    Якщо {@code true}, запускає фоновий потік для приховування вікон
     * дочірніх процесів (наприклад, вікна Python/Matplotlib).
     * @return Повний лог консолі за всю сесію у вигляді рядка.
     * @throws IOException Якщо виникла помилка при запуску процесу або роботі з потоками I/O.
     */
    public String runAllInOneSession(Path context,
                                     List<? extends ShellInteractiveRunner.BashRun> runs,
                                     boolean hide) throws IOException {
        return shellInteractiveRunner.runAllInOneSession(context, runs, hide);
    }
}