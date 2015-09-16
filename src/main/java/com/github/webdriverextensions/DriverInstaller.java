package com.github.webdriverextensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;

public class DriverInstaller {
    private final File installationDirectory;
    private final Log log;
    private final DriverVersionHandler versionHandler;

    public DriverInstaller(File installationDirectory, Log log) {
        this.installationDirectory = installationDirectory;
        this.log = log;
        this.versionHandler = new DriverVersionHandler(installationDirectory);
    }

    private static boolean directoryContainsSingleDirectory(File directory) {
        File[] files = directory.listFiles();
        return files != null && files.length == 1 && files[0].isDirectory();
    }

    private static boolean directoryContainsSingleFile(File directory) throws MojoExecutionException {
        File[] files = directory.listFiles();
        return files != null && files.length == 1 && files[0].isFile();
    }

    private static void moveDirectoryInDirectory(File from, String to) throws MojoExecutionException {
        assert directoryContainsSingleDirectory(from);
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(from, null, null, true);
            File destDir = new File(to);
            if ( destDir.exists()){
                org.apache.commons.io.FileUtils.deleteDirectory(destDir);
            }

            org.apache.commons.io.FileUtils.moveDirectory(new File(subDirectories.get(1)), destDir);
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving direcotry in directory " + Utils.quote(from) + " to " + Utils.quote(to), ex);
        }
    }

    private static void moveFileInDirectory(File from, String to) throws MojoExecutionException {
        assert directoryContainsSingleFile(from);
        try {
            List<String> files = FileUtils.getFileNames(from, null, null, true);
            FileUtils.rename(new File(files.get(0)), new File(to));
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving file in directory " + Utils.quote(from) + " to " + Utils.quote(to), ex);
        }
    }

    private static void moveAllFilesInDirectory(File from, String to) throws MojoExecutionException {
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(from, null, null, true);
            FileUtils.rename(new File(subDirectories.get(0)), new File(to));
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving direcotry " + Utils.quote(from) + " to " + Utils.quote(to), ex);
        }
    }

    public void install(Driver driver, Path extractLocation) throws MojoExecutionException {
        log.info("  Installing " + driver.getId());
        File extractLocationFile = extractLocation.toFile();
        if (directoryContainsSingleDirectory(extractLocationFile)) {
            moveDirectoryInDirectory(extractLocationFile, installationDirectory + "/" + driver.getId());
        } else if (directoryContainsSingleFile(extractLocationFile)) {
            moveFileInDirectory(extractLocationFile, installationDirectory + "/" + driver.getFileName());
            makeExecutable(installationDirectory + "/" + driver.getFileName());
        } else {
            moveAllFilesInDirectory(extractLocationFile, installationDirectory + "/" + driver.getId());
        }

        versionHandler.writeVersionFile(driver);
    }


    private static void makeExecutable(String path) {
        File file = new File(path);
        if (file.exists() && !file.canExecute()) {
            file.setExecutable(true);
        }
    }

    private boolean isInstalled(Driver driver) {
        Path path = Paths.get(installationDirectory.getPath(), driver.getId());
        return path.toFile().exists();
    }

    public boolean needInstallation(Driver driver) throws MojoExecutionException {
        return ! isInstalled(driver) || !versionHandler.isSameVersion(driver);
    }
}
