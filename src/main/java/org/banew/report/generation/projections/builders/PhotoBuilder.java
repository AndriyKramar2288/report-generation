package org.banew.report.generation.projections.builders;

import lombok.Data;
import org.banew.report.generation.services.components.ToolsSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Data
public abstract class PhotoBuilder implements AutoCloseable {

    protected String label;

    @Override
    public void close() throws Exception {}

    public abstract File build(Path contextPath, ToolsSource toolsSource) throws IOException;
}
