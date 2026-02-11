package org.banew.report.generation.projections;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
}
