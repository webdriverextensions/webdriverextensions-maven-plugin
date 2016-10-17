package com.github.webdriverextensions.newversion;

import java.nio.file.Path;

public interface FileExtractor {
    boolean isExtractable(Path file);

    void extractFile(Path file, Path toDirectory);
}
