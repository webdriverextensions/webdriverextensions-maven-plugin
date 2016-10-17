package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.webdriverextensions.Utils.quote;

public class DriverInstaller {
    private final InstallDriversMojo mojo;
    private final DriverVersionHandler versionHandler;

    public DriverInstaller(InstallDriversMojo mojo) {
        this.mojo = mojo;
        this.versionHandler = new DriverVersionHandler(mojo.installationDirectory.toPath());
    }

    public boolean needInstallation(Driver driver) throws MojoExecutionException {
        return !isInstalled(driver) || !versionHandler.isSameVersion(driver);
    }

    public void install(Driver driver, Path extractLocation) throws MojoExecutionException {
        if (extractLocation.toFile().isDirectory() && directoryIsEmpty(extractLocation)) {
            throw new InstallDriversMojoExecutionException("Failed to install driver since no files found to install", mojo, driver);
        }

        try {
            if (directoryContainsSingleFile(extractLocation)) {
                moveFileInDirectory(extractLocation, mojo.installationDirectory.toPath().resolve(driver.getFileName()));
                makeExecutable(mojo.installationDirectory.toPath().resolve(driver.getFileName()));
            } else {
                moveAllFilesInDirectory(extractLocation, mojo.installationDirectory.toPath().resolve(driver.getId()));
            }

            versionHandler.writeVersionFile(driver);
        } catch (Exception e) {
            throw new InstallDriversMojoExecutionException("Failed to install driver cause of " + e.getMessage(), e, mojo, driver);
        }

    }

    private boolean isInstalled(Driver driver) {
        Path path = mojo.installationDirectory.toPath().resolve(driver.getFileName());
        return path.toFile().exists();
    }

    private boolean directoryIsEmpty(Path directory) {
        return directory.toFile().listFiles().length == 0;
    }

    private boolean directoryContainsSingleFile(Path directory) throws MojoExecutionException {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isFile();
    }

    private void moveFileInDirectory(Path from, Path to) throws MojoExecutionException {
        assert directoryContainsSingleFile(from);
        try {
            List<String> files = FileUtils.getFileNames(from.toFile(), null, null, true);
            Path singleFile = Paths.get(files.get(0));
            mojo.getLog().info("  Moving " + quote(singleFile) + " to " + quote(to));
            Files.createDirectories(to);
            Files.move(singleFile, to.resolve(singleFile.getFileName()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to move file in directory " + quote(from) + " to " + quote(to), e);
        }
    }

    private void moveAllFilesInDirectory(Path from, Path to) throws MojoExecutionException {
        try {
            Files.createDirectories(to);
            for (File file : from.toFile().listFiles()) {
                mojo.getLog().info("  Moving " + file + " to " + to.resolve(file.toPath().getFileName()));
                Files.move(file.toPath(), to.resolve(file.toPath().getFileName()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to move directory " + quote(from) + " to " + quote(to), e);
        }
    }

    private void makeExecutable(Path path) {
        File file = path.toFile();
        if (file.exists() && !file.canExecute()) {
            file.setExecutable(true);
        }
    }
}
