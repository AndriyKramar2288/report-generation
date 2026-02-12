package org.banew.report.generation.projections.builders;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

@EqualsAndHashCode(callSuper = true)
@Data
public class DirectPhotoBuilder extends PhotoBuilder {

    private String file;

    @Override
    public File build(Path contextPath) throws IOException {

        File photo = new File(contextPath.toFile(), file);
        if (!photo.exists()
                || !photo.isFile()
                || !(photo.getName().endsWith(".png") || photo.getName().endsWith(".jpg"))) {

            throw new FileNotFoundException(file + " is not found or has a wrong format!");
        }

        return photo;
    }
}
