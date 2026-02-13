package org.banew.report.generation.cascade.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "course")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class CourseObjectModel {
    @XmlElementWrapper(name = "labs")
    @XmlElement(name = "lab")
    private List<LabModel> labs = new ArrayList<>();
}