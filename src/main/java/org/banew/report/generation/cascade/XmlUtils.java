package org.banew.report.generation.cascade;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import org.banew.report.generation.cascade.xml.CourseObjectModel;
import org.banew.report.generation.cascade.xml.LabModel;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class XmlUtils {

    private static final JAXBContext context;

    static {
        try {
            context = JAXBContext.newInstance(CourseObjectModel.class,
                    LabModel.class,
                    LabModel.LabFile.class,
                    LabModel.XmlShellCommand.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static CourseObjectModel unmashallCourseObjectModel(File xml) throws JAXBException {
        return (CourseObjectModel) context.createUnmarshaller().unmarshal(xml);
    }

    public static void generateSchema() {
        try {
            context.generateSchema(new SchemaOutputResolver() {
                @Override
                public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                    suggestedFileName = "schema.xsd";
                    StreamResult result = new StreamResult(new File(suggestedFileName));
                    result.setSystemId(suggestedFileName);
                    return result;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
