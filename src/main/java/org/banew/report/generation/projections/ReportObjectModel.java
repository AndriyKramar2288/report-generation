package org.banew.report.generation.projections;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.banew.report.generation.projections.builders.BashPhotoBuilder;
import org.banew.report.generation.projections.builders.DirectPhotoBuilder;
import org.banew.report.generation.projections.builders.FilePhotoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportObjectModel {

    private static final Logger log = LoggerFactory.getLogger(ReportObjectModel.class);

    private Map<String, String> properties = new HashMap<>();
    private List<String> codes = new ArrayList<>();
    @Valid
    private Photos photos = new Photos();

    @JsonIgnore
    private String content;
    @JsonIgnore
    private Map<String, String> codeFileNameToContentMap = new HashMap<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Photos {
        @Valid
        private Map<String, FilePhotoBuilder> text = new HashMap<>();
        @Valid
        private Map<String, BashPhotoBuilder> bash = new HashMap<>();
        @Valid
        private Map<String, DirectPhotoBuilder> images = new HashMap<>();
    }

    public static ReportObjectModel create(URI romSource, Path contextPath) {
        log.debug("Блядь, пробуєм роздуплити цей файл: {}", romSource);
        try {
            log.debug("Читаєм цю хуйню з діска, надійся, шо там не пусто");
            String content = Files.readString(Paths.get(romSource));

            log.debug("Настраюєм цей йобаний Flexmark, шоб він схавав наш YAML");
            MutableDataSet options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Collections.singletonList(YamlFrontMatterExtension.create()));
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            log.debug("Парсим цю залупу, щас буде видно, хто де насрав");
            Node document = parser.parse(content);
            Node metadataBlock = document.getFirstChild();

            if (metadataBlock instanceof YamlFrontMatterBlock) {
                log.debug("Опа, надибали метадані! Видирай цей YAML нахуй");
                String yamlText = metadataBlock.getChars().toString();

                log.debug("Запрягаєм Jackson, хай мапить цей брєд на об'єкт");
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

                var obj = mapper.readValue(yamlText, ReportObjectModel.class);

                obj.getCodes().forEach(pattern -> {
                    var files = resolveFiles(contextPath, pattern);
                    files.forEach(file -> {
                        try {
                            obj.getCodeFileNameToContentMap()
                                    .put(file.toFile().getName(), Files.readString(file));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

                log.debug("Рендерим остальну парашу в HTML, шоб було красиво, блядь");
                String htmlContent = renderer.render(
                        parser.parse(content.substring(metadataBlock.getEndOffset()).trim()));

                obj.setContent(htmlContent);

                log.debug("Дописуєм рік, бо цей підарас сам не знає, яке сьогодні число");
                obj.getProperties().put("year", LocalDateTime.now().getYear() + "");

                log.debug("Всьо, об'єкт зліпили, не розсипався — і то заєбісь");
                return obj;
            } else {
                log.debug("Сука, де метадані? Ти шо, здурів, таку парашу підсовувать?");
            }
        } catch (JacksonException jacksonException) {
            log.debug("Всьо, пізда, Джексон подавився твоїм YAML-ом: {}", jacksonException.getMessage());
            throw new RuntimeException("failed to parse note.md", jacksonException);
        } catch (IOException ioException) {
            log.debug("Якийсь гондон забрав файл або диск вмер: {}", ioException.getMessage());
            throw new RuntimeException("failed to read file", ioException);
        }

        log.debug("Вертаєм null, бо ми рукожопи і нічо не знайшли");
        return null;
    }

    private static List<Path> resolveFiles(Path rootPath, String pattern) {

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        Path relativePath = rootPath.relativize(path);
                        return matcher.matches(relativePath);
                    })
                    .collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}