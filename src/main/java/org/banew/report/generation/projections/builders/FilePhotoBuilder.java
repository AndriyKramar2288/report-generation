package org.banew.report.generation.projections.builders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.banew.report.generation.ImageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(FilePhotoBuilder.class);
    private String name;

    private static File findFileContent(String fileName, Path root) {
        log.debug("Так, блядь, починаєм шмон у теці: {}", root);
        log.debug("Шукаєм оцю залупу під назвою: '{}'", fileName);

        try (Stream<Path> stream = Files.walk(root)) {
            log.debug("Запустили стрім, щас будемо перебирати цей весь хлам на діску");

            Optional<Path> foundFile = stream
                    .filter(path -> {
                        boolean isFile = Files.isRegularFile(path);
                        if (!isFile) {
                            log.debug("Пропускаєм цю парашу, це не файл: {}", path.getFileName());
                        }
                        return isFile;
                    })
                    .filter(path -> {
                        String currentName = path.getFileName().toString();
                        boolean match = currentName.equals(fileName);
                        if (match) {
                            log.debug("ЄБАТЬ! Надибали! Ось він, сука: {}", path.toAbsolutePath());
                        }
                        return match;
                    })
                    .findFirst();

            if (foundFile.isPresent()) {
                log.debug("Всьо, капець пошукам, файл у нас в кармані. Тягнем його на білд");
                return foundFile.get().toFile();
            } else {
                log.debug("Пізда рулю... Обійшли всьо, а файла '{}' ніхуя нема в '{}'. Ти шо, гоніш?", fileName, root);
                return null;
            }
        } catch (IOException e) {
            log.debug("Якась йобана помилка вилізла, поки ми рились у смітнику: {}", e.getMessage());
            throw new RuntimeException("Помилка при пошуку файлу: " + fileName, e);
        }
    }

    @Override
    protected File buildTextFile(Path contextPath) throws IOException {
        log.debug("Визиваєм пошук файла '{}' через контекст '{}', блядь", name, contextPath);
        return findFileContent(name, contextPath);
    }
}