package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.stream.Collectors;

class DriverVersionHandler {
    private final Path installationDirectory;

    public DriverVersionHandler(Path installationDirectory) {
        this.installationDirectory = installationDirectory;
    }

    void writeVersionFile(Driver driver) throws MojoExecutionException {
        Path file = getVersionFile(driver);
        String versionString = driver.toString();

        try {
            Files.write(file, Collections.singleton(versionString), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to create version file containing metadata about the installed driver" + Utils.debugInfo(driver), e);
        }
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
            String savedVersion = Files.lines(versionFile).collect(Collectors.joining());
            return driver.equals(Driver.fromJson(savedVersion));
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to compare installed driver version with the driver version to install" + Utils.debugInfo(driver), e);
        }
    }
}
