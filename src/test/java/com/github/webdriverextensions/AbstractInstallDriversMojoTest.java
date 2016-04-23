package com.github.webdriverextensions;

import org.junit.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static com.github.webdriverextensions.Utils.*;

public abstract class AbstractInstallDriversMojoTest extends AbstractMojoTestCase {

    private InstallDriversMojo mojo;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteTempDirectory();
        deleteInstallationDirectory();
    }

    public MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }

    public InstallDriversMojo getMojo(String pomPath, String goal) throws Exception {
        MavenProject project = getMavenProject(pomPath);
        InstallDriversMojo mojo = (InstallDriversMojo) lookupConfiguredMojo(project, goal);

        // some global test preparations
        this.mojo = mojo;

        // delete download directories before running test
        deleteTempDirectory();
        deleteInstallationDirectory();

        logTestName(mojo);

        return mojo;
    }

    private void deleteTempDirectory() throws IOException {
        if (mojo.tempDirectory != null) {
            FileUtils.deleteDirectory(mojo.tempDirectory);
        }
    }

    private void deleteInstallationDirectory() throws IOException {
        if (mojo.installationDirectory != null) {
            FileUtils.deleteDirectory(mojo.installationDirectory);
        }
    }

    public void logTestName(InstallDriversMojo mojo) {
        System.out.println("");
        System.out.println("");
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        mojo.getLog().info("## TEST: " + stackTrace[2].getFileName().replace(".java", "") + "." + stackTrace[2].getMethodName() + " on platform "
                + currentPlatform() + " " + currentBit() + "BIT");
    }

    private static String currentPlatform() {
        if (isMac()) {
            return "MAC";
        } else if (isWindows()) {
            return "WINDOWS";
        } else if (isLinux()) {
            return "LINUX";
        }
        throw new IllegalStateException("Unsupported OS, OS is neither MAC, WINDOWS nor LINUX");
    }

    private static String currentBit() {
        return is64Bit() ? "64" : "32";
    }

    public FileTime getDriverCreationTime(String driver) {
        for (File file : mojo.installationDirectory.listFiles()) {
            if (file.getName().equals(driver)) {
                BasicFileAttributes attr = null;
                try {
                    attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                } catch (IOException e) {
                    fail("Did not find driver creation time for driver " + driver + " since driver file or folder is not installed"
                            + System.lineSeparator() + Utils.debugInfo(mojo));
                }
                return attr.creationTime();
            }
        }
        fail("Did not find driver creation time for driver " + driver + " since driver file or folder is not installed"
                + System.lineSeparator() + Utils.debugInfo(mojo));
        return null;
    }

    public void assertDriverIsInstalled(String driverFileName) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        for (File file : mojo.installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(driverFileName.replace(".exe", "") + ".version")) {
                foundDriverVersionFile = true;
            }
        }
        if (!foundDriverFile) {
            fail("Driver with file name " + driverFileName + " was not found in the installation directory"
                    + System.lineSeparator() + Utils.debugInfo(mojo));
        }
        if (!foundDriverVersionFile) {
            fail("Driver version file with file name " + driverFileName + ".version was not found in the installation directory"
                    + System.lineSeparator() + Utils.debugInfo(mojo));
        }
    }

    public void assertDriverIsNotInstalled(String driverFileName) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        for (File file : mojo.installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(driverFileName + ".version")) {
                foundDriverVersionFile = true;
            }
        }
        if (foundDriverFile) {
            fail("Driver with file name " + driverFileName + " was found in the installation directory when it should not have been"
                    + System.lineSeparator() + Utils.debugInfo(mojo));
        }
        if (foundDriverVersionFile) {
            fail("Driver version file with file name " + driverFileName + ".version was not found in the installation directory when it should not have been"
                    + System.lineSeparator() + Utils.debugInfo(mojo));
        }
    }

    public void assertNumberOfInstalledDriverIs(int numberOfDrivers) {
        if (mojo.installationDirectory.listFiles().length != numberOfDrivers * 2) {
            fail("Number of drivers installed is not " + numberOfDrivers
                    + System.lineSeparator() + Utils.debugInfo(mojo));
        }
    }

    public void fakePlatformToBeLinux() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "linux");
    }

    public void fakePlatformToBeWindows() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "windows");
    }

    public void fakePlatformToBeMac() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "mac");
    }

    public void fakeBitToBe64() {
        System.setProperty(FAKED_BIT_PROPERTY_KEY, "64");
    }

    public void fakeBitToBe32() {
        System.setProperty(FAKED_BIT_PROPERTY_KEY, "32");
    }


    public static void fail(String message) {
        Assert.fail("[" + currentPlatform() + " " + currentBit() + "BIT] " + message);
    }
}
