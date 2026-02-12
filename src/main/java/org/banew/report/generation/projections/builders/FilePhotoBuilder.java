package org.banew.report.generation.projections.builders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.banew.report.generation.ImageGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePhotoBuilder extends TextContainingPhotoBuilder {

    private String name;

    private static File findFileContent(String fileName, Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            // 2. Шукаємо файл за назвою
            Optional<Path> foundFile = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst();

            // 3. Якщо знайшли — читаємо, якщо ні — повертаємо порожній рядок або кидаємо помилку
            if (foundFile.isPresent()) {
                System.out.println("Файл знайдено за шляхом: " + foundFile.get().toAbsolutePath());
                return foundFile.get().toFile();
            } else {
                System.err.println("Файл з назвою " + fileName + " не знайдено в " + root);
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Помилка при пошуку файлу: " + fileName, e);
        }
    }

    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        return findFileContent(name, contextPath);
    }
}
