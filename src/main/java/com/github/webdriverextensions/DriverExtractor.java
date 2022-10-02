package com.github.webdriverextensions;

import com.github.webdriverextensions.newversion.FileExtractor;
import com.github.webdriverextensions.newversion.FileExtractorImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static com.github.webdriverextensions.Utils.quote;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DriverExtractor {
    private final Log log;
    private final Path tempDirectory;

    Path extractDriver(Driver driver, Path downloadedFile) throws MojoExecutionException {
        FileExtractor fileExtractor = new FileExtractorImpl(driver.getFileMatchInside());
        Path extractDirectory = tempDirectory.resolve(driver.getDriverDownloadDirectoryName());

        try {
            Files.createDirectories(extractDirectory);
            if (fileExtractor.isExtractable(downloadedFile)) {
                log.info("  Extracting " + quote(downloadedFile) + " to temp folder");
                fileExtractor.extractFile(downloadedFile, extractDirectory);
            } else {
                log.info("  Copying " + quote(downloadedFile) + " to temp folder");
                Files.copy(downloadedFile, extractDirectory.resolve(downloadedFile.getFileName()));
            }
            return extractDirectory;
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to extract driver from " + quote(downloadedFile), driver, e);
        }
    }
}
