package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DriverVersionHandler {
    private final Path installationDirectory;

    void writeVersionFile(Driver driver) throws MojoExecutionException {
        Path file = getVersionFile(driver);
        String versionString = driver.toString();

        try {
            Files.write(file, Collections.singleton(versionString), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to create version file containing metadata about the installed driver", driver, e);
        }
    }

    private Path getVersionFile(Driver driver) {
        return installationDirectory.resolve(driver.getId() + ".version");
    }

    boolean isSameVersion(Driver driver) throws MojoExecutionException {
        try {
            Path versionFile = getVersionFile(driver);
            if (!versionFile.toFile().isFile() || !versionFile.toFile().canRead()) {
                return false;
            }
            String savedVersion = Files.lines(versionFile).collect(Collectors.joining());
            return driver.equals(Driver.fromJson(savedVersion));
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to compare installed driver version with the driver version to install", driver, e);
        }
    }
}
