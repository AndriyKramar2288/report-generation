package org.banew.report.generation.projections;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface PhotoBuilder {
    File build(Path contextPath) throws IOException;
}
