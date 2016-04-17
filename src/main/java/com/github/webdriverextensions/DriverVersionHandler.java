package com.github.webdriverextensions;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;

class DriverVersionHandler {
    private final File installationDirectory;

    public DriverVersionHandler(File installationDirectory) {
        this.installationDirectory = installationDirectory;
    }

    void writeVersionFile(Driver driver) throws MojoExecutionException {
        File file = getVersionFile(driver);
        String versionString = createVersionString(driver);

        try {
            org.apache.commons.io.FileUtils.writeStringToFile(file, versionString);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create version file containing metadata about the installed driver" + "\ndriver:\n" + driver, e);
        }
    }

    private String createVersionString(Driver driver) {
        return driver.toString();
    }

    private File getVersionFile(Driver driver) {
        return new File(installationDirectory + "/" + driver.getId() + ".version");
    }

    public boolean isSameVersion(Driver driver) throws MojoExecutionException {
        try {
            File versionFile = getVersionFile(driver);
            if (!versionFile.exists()) {
                return false;
            }
            String savedVersion = org.apache.commons.io.FileUtils.readFileToString(versionFile);
            String currentVersion = createVersionString(driver);
            return savedVersion.equals(currentVersion);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to compare installed driver version with the driver version to install" + "\ndriver:\n" + driver, e);
        }
    }
}
