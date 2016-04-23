package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.webdriverextensions.Utils.quote;

public class DriverInstaller {
    private final InstallDriversMojo mojo;
    private final DriverVersionHandler versionHandler;

    public DriverInstaller(InstallDriversMojo mojo) {
        this.mojo = mojo;
        this.versionHandler = new DriverVersionHandler(mojo.installationDirectory);
    }

    public boolean needInstallation(Driver driver) throws MojoExecutionException {
        return !isInstalled(driver) || !versionHandler.isSameVersion(driver);
    }

    public void install(Driver driver, Path extractLocation) throws MojoExecutionException {
        if (directoryIsEmpty(extractLocation)) {
            throw new InstallDriversMojoExecutionException("Failed to install driver since no files found to install", mojo, driver);
        }

        try {
            if (directoryContainsSingleDirectory(extractLocation)) {
                moveDirectoryInDirectory(extractLocation, Paths.get(mojo.installationDirectory.getPath(), driver.getId()));
            } else if (directoryContainsSingleFile(extractLocation)) {
                moveFileInDirectory(extractLocation, Paths.get(mojo.installationDirectory.getPath(), driver.getFileName()));
                makeExecutable(Paths.get(mojo.installationDirectory.getPath(), driver.getFileName()));
            } else {
                moveAllFilesInDirectory(extractLocation, Paths.get(mojo.installationDirectory.getPath(), driver.getId()));
            }

            versionHandler.writeVersionFile(driver);
        } catch (Exception e) {
            throw new InstallDriversMojoExecutionException("Failed to install driver cause of " + e.getMessage(), e, mojo, driver);
        }

    }

    private boolean isInstalled(Driver driver) {
        Path path = Paths.get(mojo.installationDirectory.getPath(), driver.getFileName());
        return path.toFile().exists();
    }

    private boolean directoryIsEmpty(Path directory) {
        return directory.toFile().listFiles().length == 0;
    }

    private boolean directoryContainsSingleDirectory(Path directory) {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isDirectory();
    }

    private boolean directoryContainsSingleFile(Path directory) throws MojoExecutionException {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isFile();
    }

    private void moveDirectoryInDirectory(Path from, Path to) throws MojoExecutionException {
        assert directoryContainsSingleDirectory(from);
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(from.toFile(), null, null, true);
            if (to.toFile().exists()) {
                org.apache.commons.io.FileUtils.deleteDirectory(to.toFile());
            }

            File singleDirectory = new File(subDirectories.get(1));
            mojo.getLog().info("  Moving " + quote(singleDirectory) + " to " + quote(to));
            org.apache.commons.io.FileUtils.moveDirectory(singleDirectory, to.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to move directory in directory " + quote(from) + " to " + quote(to), e);
        }
    }

    private void moveFileInDirectory(Path from, Path to) throws MojoExecutionException {
        assert directoryContainsSingleFile(from);
        try {
            List<String> files = FileUtils.getFileNames(from.toFile(), null, null, true);
            File singleFile = new File(files.get(0));
            mojo.getLog().info("  Moving " + quote(singleFile) + " to " +   quote(to));
            FileUtils.rename(singleFile, to.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to move file in directory " + quote(from) + " to " + quote(to), e);
        }
    }

    // TODO: Investigate if this method does what it should do, should the method name be changed or the method implementation
    private void moveAllFilesInDirectory(Path from, Path to) throws MojoExecutionException {
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(from.toFile(), null, null, true);
            mojo.getLog().info("  Moving " + subDirectories.get(0) + " to " + to);
            FileUtils.rename(new File(subDirectories.get(0)), to.toFile());
        } catch (IOException e) {
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
