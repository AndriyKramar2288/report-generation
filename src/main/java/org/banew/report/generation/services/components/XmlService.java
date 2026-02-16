package org.banew.report.generation.services.components;

import com.google.inject.Singleton;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import org.banew.report.generation.cascade.xml.LabModel;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class XmlService {

    private final JAXBContext context;

    public XmlService() {
        try {
            context = JAXBContext.newInstance(CourseObjectModel.class,
                    LabModel.class,
                    LabModel.LabFile.class,
                    LabModel.XmlShellCommand.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public CourseObjectModel unmashallCourseObjectModel(File xml) throws JAXBException {
        return (CourseObjectModel) context.createUnmarshaller().unmarshal(xml);
    }

    public void generateSchema(Path root) {
        try {
            context.generateSchema(new SchemaOutputResolver() {
                @Override
                public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                    Path outputPath = root.resolve("schema.xsd");
                    StreamResult result = new StreamResult(Files.newOutputStream(outputPath));
                    result.setSystemId(outputPath.toUri().toASCIIString());
                    return result;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}