package com.github.webdriverextensions.newversion;

import java.nio.file.Path;

public interface FileExtractor {
    void extractFile(Path file, Path toDirectory);
}
