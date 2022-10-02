package com.github.webdriverextensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;

import static com.github.webdriverextensions.Utils.quote;

class DriverInstaller {
    private final Path installationDirectory;
    private final Log log;
    private final DriverVersionHandler versionHandler;
    
    @Setter
    private Properties driverPathProperyTarget;

    DriverInstaller(Log log, Path installationDirectory) {
        this.log = log;
        this.installationDirectory = installationDirectory;
        this.versionHandler = new DriverVersionHandler(installationDirectory);
    }

    boolean needInstallation(Driver driver) {
        try {
            return !isInstalled(driver) || !versionHandler.isSameVersion(driver);
        } catch (MojoExecutionException ex) {
            log.warn("Could not determine if same version of driver is already installed, will install it again", ex);
            return true;
        }
    }

    void install(Driver driver, Path extractLocation) throws MojoExecutionException {
        log.info("start installing : " + driver.getName() + " from " + extractLocation);
        if (extractLocation.toFile().isDirectory() && directoryIsEmpty(extractLocation)) {
            throw new InstallDriversMojoExecutionException("Failed to install driver since no files found to install", driver, null);
        }

        try {
            Files.createDirectories(installationDirectory);
            if (directoryContainsSingleDirectory(extractLocation)) {
                Path singleDirectory = extractLocation.toFile().listFiles()[0].toPath();
                moveAllFilesInDirectory(singleDirectory, installationDirectory.resolve(driver.getId()));
            } else if (directoryContainsSingleFile(extractLocation)) {
                String newFileName = driver.getFileName();
                moveFileInDirectory(extractLocation, installationDirectory, newFileName);
                makeExecutable(installationDirectory.resolve(newFileName));
                setDriverPathProperty(driver, installationDirectory.resolve(newFileName));
            } else {
                moveAllFilesInDirectory(extractLocation, installationDirectory.resolve(driver.getId()));
            }

            versionHandler.writeVersionFile(driver);
        } catch (IOException | MojoExecutionException e) {
            throw new InstallDriversMojoExecutionException("Failed to install driver", driver, e);
        }

    }

    void setDriverPathPropertyIfInstalled(Driver driver) {
        if (isInstalled(driver)) {
            setDriverPathProperty(driver, installationDirectory.resolve(driver.getFileName()));
        }
    }
    
    private void setDriverPathProperty(Driver driver, Path location) {
        final String driverName = driver.getName().toLowerCase();
        String propertyName = "";
        if (driverName.startsWith("chrome") || driverName.startsWith("chromium")) {
            propertyName = "chrome";
        } else if (driverName.startsWith("opera")) {
            propertyName = "opera";
        } else if (driverName.startsWith("internetexplorer")) {
            propertyName = "ie";
        } else if (driverName.startsWith("edge")) {
            propertyName = "edge";
        } else if (driverName.startsWith("gecko") || driverName.startsWith("firefox")) {
            propertyName = "gecko";
        }
        if (driverPathProperyTarget != null && !propertyName.isEmpty() && driver.getPlatform().equalsIgnoreCase(Utils.detectPlatform())) {
            driverPathProperyTarget.putIfAbsent(String.format("webdriver.%s.driver", propertyName), location.toAbsolutePath().toString());
        }
    }

    private boolean isInstalled(Driver driver) {
        Path path = installationDirectory.resolve(driver.getFileName());
        return path.toFile().exists();
    }

    private boolean directoryIsEmpty(Path directory) {
        return directory.toFile().listFiles().length == 0;
    }

    private boolean directoryContainsSingleFile(Path directory) throws MojoExecutionException {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isFile();
    }

    private boolean directoryContainsSingleDirectory(Path directory) {
        File[] files = directory.toFile().listFiles();
        return files != null && files.length == 1 && files[0].isDirectory();
    }

    private void moveFileInDirectory(Path from, Path to, String newFileName) throws MojoExecutionException {
        assert directoryContainsSingleFile(from);
        try {
            File[] files = from.toFile().listFiles();
            Path singleFile = files[0].toPath();
            log.info("  Moving (one File) " + quote(singleFile) + " to " + quote(to.resolve(newFileName)));
            FileUtils.forceDelete(to.resolve(newFileName).toFile());
            Files.move(singleFile, to.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to move file in directory " + quote(from) + " to " + quote(to.resolve(newFileName)), e);
        }
    }

    private void moveAllFilesInDirectory(Path from, Path to) throws MojoExecutionException {
        try {
            Files.createDirectories(to);
            for (File file : from.toFile().listFiles()) {
                log.info("  Moving (All Files) " + file + " to " + to.resolve(file.toPath().getFileName()));
                FileUtils.forceDelete(to.resolve(file.toPath().getFileName()).toFile());
                if (Os.isFamily(Os.FAMILY_WINDOWS) && file.isDirectory()) {
                    // (on windows) it is not possible to move a non-empty directory (DirectoryNotEmptyException). copy and delete should be used instead.
                    FileUtils.copyDirectory(file, to.resolve(file.toPath().getFileName()).toFile());
                    FileUtils.forceDelete(file);
                } else {
                    Files.move(file.toPath(), to.resolve(file.toPath().getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
                makeExecutable(to.resolve(file.toPath().getFileName()));
            }
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to move directory " + quote(from) + " to " + quote(to), e);
        }
    }

    private void makeExecutable(Path path) {
        File file = path.toFile();
        if (file.exists() && !file.canExecute()) {
            file.setExecutable(true);
        }
    }
}
