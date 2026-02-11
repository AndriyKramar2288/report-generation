package org.banew.report.generation;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.banew.report.generation.projections.ReportObjectModel;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;

public class Entrypoint {
    public static void main(String[] args) throws Exception {
        try {
            URI romSource = Entrypoint.class.getResource("/testRom.md").toURI(); // args[0];

            var rom = buildRom(romSource);
            ReportBuilder.generate(rom);
            System.out.println("rom: " + rom);
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static ReportObjectModel buildRom(URI romSource) {
        try {
            String content = Files.readString(Paths.get(romSource));

            MutableDataSet options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Collections.singletonList(YamlFrontMatterExtension.create()));
            Parser parser = Parser.builder(options).build();

            Node document = parser.parse(content);
            Node metadataBlock = document.getFirstChild();

            if (metadataBlock instanceof YamlFrontMatterBlock) {
                String yamlText = metadataBlock.getChars().toString();

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

                var obj = mapper.readValue(yamlText, ReportObjectModel.class);
                obj.setContent(content.substring(metadataBlock.getEndOffset()));
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
