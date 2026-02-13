
@XmlSchema(
        namespace = "com.banew/report-generation/course-object-model",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "gn", namespaceURI = "com.banew/report-generation/course-object-model")
        }
)
package org.banew.report.generation.cascade.xml;

import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;