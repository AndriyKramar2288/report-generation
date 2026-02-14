package org.banew.report.generation.cascade.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import org.banew.report.generation.services.ShellRunner;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class LabModel {
    @XmlElementWrapper(name = "runs")
    @XmlElement(name = "run")
    private List<XmlShellCommand> shellCommands = new ArrayList<>();
    @XmlElement(name = "report", required = true)
    private String report;
    @XmlElement(name = "file")
    @XmlElementWrapper(name = "files")
    private List<LabFile> files = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XmlShellCommand implements ShellRunner.BashRun {
        @XmlAttribute(name = "command")
        private String command;
        @XmlAttribute(name = "input")
        private String input;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LabFile {
        @XmlAttribute(name = "name")
        private String name;
        @XmlElement(name = "content")
        private String content;
    }
}