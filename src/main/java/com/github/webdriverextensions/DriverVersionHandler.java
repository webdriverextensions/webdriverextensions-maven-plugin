package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Path;

class DriverVersionHandler {
    private final Path installationDirectory;

    public DriverVersionHandler(Path installationDirectory) {
        this.installationDirectory = installationDirectory;
    }

    void writeVersionFile(Driver driver) throws MojoExecutionException {
        Path file = getVersionFile(driver);
        String versionString = createVersionString(driver);

        try {
            org.apache.commons.io.FileUtils.writeStringToFile(file.toFile(), versionString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create version file containing metadata about the installed driver" + Utils.debugInfo(driver), e);
        }
    }

    private String createVersionString(Driver driver) {
        return driver.toString();
    }

    private Path getVersionFile(Driver driver) {
        return installationDirectory.resolve(driver.getId() + ".version");
    }

    public boolean isSameVersion(Driver driver) throws MojoExecutionException {
        try {
            Path versionFile = getVersionFile(driver);
            if (!versionFile.toFile().exists()) {
                return false;
            }
            String savedVersion = org.apache.commons.io.FileUtils.readFileToString(versionFile.toFile());
            String currentVersion = createVersionString(driver);
            return savedVersion.equals(currentVersion);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to compare installed driver version with the driver version to install" + Utils.debugInfo(driver), e);
        }
    }
}
