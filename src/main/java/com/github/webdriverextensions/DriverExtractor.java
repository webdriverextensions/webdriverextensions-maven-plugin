package com.github.webdriverextensions;

import com.github.webdriverextensions.newversion.FileExtractor;
import com.github.webdriverextensions.newversion.FileExtractorImpl;
import org.apache.maven.plugin.MojoExecutionException;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.webdriverextensions.Utils.quote;

class DriverExtractor {
    private final InstallDriversMojo mojo;

    DriverExtractor(InstallDriversMojo mojo) {
        this.mojo = mojo;
    }

    Path extractDriver(Driver driver, Path downloadedFile) throws MojoExecutionException {
        FileExtractor fileExtractor = new FileExtractorImpl(driver.getFileMatchInside());

        try {
            if (fileExtractor.isExtractable(downloadedFile)) {
                mojo.getLog().info("  Extracting " + quote(downloadedFile) + " to temp folder");
                fileExtractor.extractFile(downloadedFile, mojo.tempDirectory.toPath());
            } else {
                mojo.getLog().info("  Copying " + quote(downloadedFile) + " to temp folder");
                Files.createDirectories(mojo.tempDirectory.toPath());
                Files.copy(downloadedFile, mojo.tempDirectory.toPath().resolve(downloadedFile.getFileName()));
            }
            if (!mojo.keepDownloadedWebdrivers) {
                Files.delete(downloadedFile);
            }
            return mojo.tempDirectory.toPath();
        } catch (Exception e) {
            throw new InstallDriversMojoExecutionException("Failed to extract driver from " + quote(downloadedFile) + " cause of " + e.getMessage(), e, mojo, driver);
        }
    }
}
