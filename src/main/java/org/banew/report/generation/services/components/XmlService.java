package org.banew.report.generation.services.components;

import com.google.inject.Singleton;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import org.banew.report.generation.cascade.xml.LabModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class XmlService {

    private static final Logger log = LoggerFactory.getLogger(XmlService.class);
    private final JAXBContext context;

    public XmlService() {
        try {
            log.debug("Initializing JAXB context for CourseObjectModel and LabModel...");
            context = JAXBContext.newInstance(CourseObjectModel.class,
                    LabModel.class,
                    LabModel.LabFile.class,
                    LabModel.XmlShellCommand.class);
        } catch (JAXBException e) {
            log.error("Critical error during JAXB context initialization", e);
            throw new RuntimeException(e);
        }
    }

    public CourseObjectModel unmashallCourseObjectModel(File xml) throws JAXBException {
        log.debug("Unmarshalling CourseObjectModel from file: {}", xml.getName());
        return (CourseObjectModel) context.createUnmarshaller().unmarshal(xml);
    }

    public void generateSchema(Path root) {
        log.debug("Generating XML Schema (XSD) at location: {}", root);
        try {
            context.generateSchema(new SchemaOutputResolver() {
                @Override
                public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                    Path outputPath = root.resolve("schema.xsd");
                    log.info("Creating XSD schema file: {}", outputPath.toAbsolutePath());
                    StreamResult result = new StreamResult(Files.newOutputStream(outputPath));
                    result.setSystemId(outputPath.toUri().toASCIIString());
                    return result;
                }
            });
        } catch (IOException e) {
            log.error("Failed to generate XML schema", e);
            throw new RuntimeException(e);
        }
    }
}