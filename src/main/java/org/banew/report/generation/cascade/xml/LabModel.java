package org.banew.report.generation.cascade.xml;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.xml.bind.annotation.*;
import lombok.Data;
import org.banew.report.generation.services.components.ShellRunner;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class LabModel {
    @XmlElementWrapper(name = "runs")
    @XmlElement(name = "run")
    private List<XmlShellCommand> shellCommands = new ArrayList<>();
    @XmlElement(name = "report", required = true)
    @NotBlank(message = "Наявність звіту обов'язкова!")
    private String report;
    @XmlElement(name = "file")
    @XmlElementWrapper(name = "files")
    @Valid
    private List<LabFile> files = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XmlShellCommand implements ShellRunner.BashRun {
        @XmlAttribute(name = "command", required = true)
        @NotBlank(message = "Раз ви оголосили виклик, то він не може бути пустим!")
        private String command;
        @XmlAttribute(name = "input")
        private String input;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LabFile {
        @NotBlank(message = "Назва файлу не може бути пустою!")
        @XmlAttribute(name = "name", required = true)
        private String name;
        @NotBlank(message = "Вміст файлу не може бути пустим!")
        @XmlValue
        private String content;
    }
}