package org.banew.report.generation.projections;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportObjectModel {

    private Map<String, String> properties;
    private Photos photos;
    private String content;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Photos {
        private Map<String, FilePhotoBuilder> files = new HashMap<>();
    }

    protected ReportObjectModel() {}

    public static ReportObjectModel create(URI romSource) {
        try {
            String content = Files.readString(Paths.get(romSource));

            MutableDataSet options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Collections.singletonList(YamlFrontMatterExtension.create()));
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            Node document = parser.parse(content);
            Node metadataBlock = document.getFirstChild();

            if (metadataBlock instanceof YamlFrontMatterBlock) {
                String yamlText = metadataBlock.getChars().toString();

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

                var obj = mapper.readValue(yamlText, ReportObjectModel.class);

                String htmlContent = renderer.render(
                        parser.parse(content.substring(metadataBlock.getEndOffset()).trim()));

                obj.setContent(htmlContent);
                obj.getProperties().put("year", LocalDateTime.now().getYear() + "");

                return obj;
            }
        } catch (JacksonException jacksonException) {
            throw new RuntimeException("failed to parse note.md", jacksonException);
        } catch (IOException ioException) {
            throw new RuntimeException("failed to read file", ioException);
        }
        return null;
    }
}
