package org.banew.report.generation.projections;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@EqualsAndHashCode(callSuper = true)
@Data
public class BashPhotoBuilder extends PhotoBuilder {
    @Override
    public File build(Path contextPath) throws IOException {
        return null;
    }
}
