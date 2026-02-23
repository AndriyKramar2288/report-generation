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
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
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
        log.debug("Attempting to initialize ReportObjectModel from source: {}", romSource);
        try {
            log.debug("Reading source file content from disk...");
            String content = Files.readString(Paths.get(romSource), StandardCharsets.UTF_8);

            log.debug("Configuring Flexmark parser with YAML Front Matter extension.");
            MutableDataSet options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Collections.singletonList(YamlFrontMatterExtension.create()));
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            log.debug("Parsing document structure...");
            Node document = parser.parse(content);
            Node metadataBlock = document.getFirstChild();

            if (metadataBlock instanceof YamlFrontMatterBlock) {
                log.debug("Metadata block identified. Extracting YAML content.");
                String yamlText = metadataBlock.getChars().toString();

                log.debug("Initializing Jackson YAML mapper for object mapping.");
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

                var obj = mapper.readValue(yamlText, ReportObjectModel.class);

                log.debug("Resolving source code files based on defined patterns.");
                obj.getCodes().forEach(pattern -> {
                    var files = resolveFiles(contextPath, pattern);
                    files.forEach(file -> {
                        try {
                            String codeContent;
                            try {
                                codeContent = Files.readString(file, StandardCharsets.UTF_8);
                            } catch (MalformedInputException e) {
                                log.warn("File {} is not UTF-8 encoded. Attempting fallback to Windows-1251.", file.getFileName());
                                codeContent = Files.readString(file, Charset.forName("windows-1251"));
                            }

                            obj.getCodeFileNameToContentMap().put(file.toFile().getName(), codeContent);

                        } catch (IOException e) {
                            log.error("Failed to read source code file {}: {}", file, e.getMessage());
                            throw new RuntimeException(e);
                        }
                    });
                });

                log.debug("Rendering Markdown body to HTML format.");
                String htmlContent = renderer.render(
                        parser.parse(content.substring(metadataBlock.getEndOffset()).trim()));

                obj.setContent(htmlContent);

                log.debug("Injecting current year into report properties.");
                obj.getProperties().put("year", LocalDateTime.now().getYear() + "");

                log.info("ReportObjectModel successfully built and validated.");
                return obj;
            } else {
                log.error("Metadata block missing. Ensure the file starts with YAML front matter (---).");
            }
        } catch (JacksonException jacksonException) {
            log.error("YAML parsing failed: {}", jacksonException.getMessage());
            throw new RuntimeException("Failed to parse YAML front matter in note.md", jacksonException);
        } catch (IOException ioException) {
            log.error("IO error occurred during file processing: {}", ioException.getMessage());
            throw new RuntimeException("Failed to read report source file", ioException);
        }

        log.warn("Report creation failed. Returning null.");
        return null;
    }

    private static List<Path> resolveFiles(Path rootPath, String pattern) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> !path.toString().endsWith(".class"))
                    .filter(path -> {
                        Path relativePath = rootPath.relativize(path);
                        return matcher.matches(relativePath);
                    })
                    .collect(Collectors.toList());
        }
        catch (IOException e) {
            log.error("Error walking file tree for pattern: {}", pattern);
            throw new RuntimeException(e);
        }
    }
}