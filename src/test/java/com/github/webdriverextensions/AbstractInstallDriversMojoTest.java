package com.github.webdriverextensions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.github.webdriverextensions.Utils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public abstract class AbstractInstallDriversMojoTest extends AbstractMojoTestCase {

    private InstallDriversMojo mojo;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        deleteTempDirectory();
        deleteInstallationDirectory();
    }

    private MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }

    InstallDriversMojo getMojo(String pomPath) throws Exception {
        MavenProject project = getMavenProject(pomPath);
        InstallDriversMojo mojo = Mockito.spy((InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers"));

        // some global test preparations
        this.mojo = mojo;

        // delete download directories before running test
        deleteTempDirectory();
        deleteInstallationDirectory();

        logTestName(mojo);

        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository-3.0.json");
        mojo.keepDownloadedWebdrivers = true;
        DriverDownloader dlMock = Mockito.mock(DriverDownloader.class);
        when(dlMock.downloadFile(any(Driver.class), any(Path.class))).thenAnswer(new DownloadAnswer());
        doReturn(dlMock).when(mojo).getDownloader();

        return mojo;
    }

    private void deleteTempDirectory() throws IOException {
        if (mojo.tempDirectory != null && mojo.tempDirectory.toFile().exists()) {
            FileUtils.deleteDirectory(mojo.tempDirectory.toFile());
        }
    }

    private void deleteInstallationDirectory() throws IOException {
        if (mojo.installationDirectory.toPath() != null && mojo.installationDirectory.exists()) {
            FileUtils.deleteDirectory(mojo.installationDirectory);
        }
    }

    private void logTestName(InstallDriversMojo mojo) {
        System.out.println("");
        System.out.println("");
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        mojo.getLog().info("## TEST: " + stackTrace[2]
                .getFileName()
                .replace(".java", "") + "." + stackTrace[2].getMethodName() + " on platform "
                           + currentPlatform()  + " " + currentBit() + "BIT");
    }

    private static String currentPlatform() {
        if (isMac()) {
            return "MAC";
        } else if (isWindows10()) {
            return "WINDOWS";
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

    FileTime getDriverCreationTime(String driver) {
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

    void assertDriverIsInstalled(String driverFileName) {
        assertDriverIsInstalled(driverFileName, null);
    }

    void assertDriverIsInstalled(String driverFileName, String version) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        String versionFilename = driverFileName.replace(".exe", "") + ".version";
        for (File file : mojo.installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(versionFilename)) {
                foundDriverVersionFile = true;
                if (version != null) {
                    try {
                        String versionFileString = FileUtils.readFileToString(file, Charset.defaultCharset());
                        if (!versionFileString.contains("\"version\": \"" + version + "\"")) {
                            fail("Version " + version + " was not found in version file, version file content: " + versionFileString);
                        }
                    } catch (IOException e) {
                        fail("Failed to read version file " + versionFilename);
                    }
                }
            }
        }
        if (!foundDriverFile) {
            fail("Driver with file name " + driverFileName + " was not found in the installation directory"
                 + System.lineSeparator() + Utils.debugInfo(mojo));
        }
        if (!foundDriverVersionFile) {
            fail("Driver version file with file name " + versionFilename + " was not found in the installation directory"
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

    void assertNumberOfInstalledDriverIs(int numberOfDrivers) {
        int length = mojo.installationDirectory.listFiles().length;
        if (length != numberOfDrivers * 2) {
            fail("Number of drivers installed is not " + numberOfDrivers + ", it is " + (length / 2)
                 + System.lineSeparator() + Utils.debugInfo(mojo));
        }
    }

    void fakePlatformToBeLinux() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "linux");
    }

    void fakePlatformToBeWindows() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "windows");
    }

    void fakePlatformToBeWindows10() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "windows10");
    }

    void fakePlatformToBeMac() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "mac");
    }

    void fakeBitToBe64() {
        System.setProperty(FAKED_BIT_PROPERTY_KEY, "64");
    }

    void fakeBitToBe32() {
        System.setProperty(FAKED_BIT_PROPERTY_KEY, "32");
    }


    public static void fail(String message) {
        Assert.fail("[" + currentPlatform() + " " + currentBit() + "BIT] " + message);
    }
    
    private class DownloadAnswer implements Answer<Path> {

        @Override
        public Path answer(InvocationOnMock invocation) throws Throwable {
            Driver driver = invocation.getArgument(0);
            Path downloadDirectory = invocation.getArgument(1);
            Path downloadFilePath = downloadDirectory.resolve(driver.getFilenameFromUrl());
            // copy matching fake-driver to downloadFilePath
            Path fakeDriverDir = Paths.get("src/test/resources/fake-drivers");
            Files.createDirectories(downloadDirectory);
            switch (driver.getName()) {
                case "chromedriver":
                case "chromedriver-beta":
                case "custom-chrome-driver":
                    Files.copy(fakeDriverDir.resolve("chromedriver").resolve(driver.getFilenameFromUrl()), downloadFilePath);
                    break;
                case "geckodriver": {
                    String osArch = "";
                    if (driver.getPlatform().equalsIgnoreCase("linux")) {
                        osArch = "linux" + driver.getBit() + ".tar.gz";
                    }
                    if (driver.getPlatform().equalsIgnoreCase("windows")) {
                        osArch = "win" + driver.getBit() + ".zip";
                    }
                    if (driver.getPlatform().equalsIgnoreCase("mac")) {
                        osArch = "macos.tar.gz";
                    }
                    Files.copy(fakeDriverDir.resolve("geckodriver").resolve("geckodriver-v0.11.1-" + osArch), downloadFilePath);
                }
                break;
                case "internetexplorerdriver":
                    if (driver.getBit().equalsIgnoreCase("32")) {
                        Files.copy(fakeDriverDir.resolve("internetexplorerdriver").resolve("IEDriverServer_Win32_2.53.1.zip"), downloadFilePath);
                    } else {
                        Files.copy(fakeDriverDir.resolve("internetexplorerdriver").resolve("IEDriverServer_x64_2.53.1.zip"), downloadFilePath);
                    }
                    break;
                case "phantomjs":
                case "custom-phantomjs-driver-filematchinside":
                case "custom-phantomjs-driver": {
                    String osArch = "";
                    if (driver.getPlatform().equalsIgnoreCase("linux")) {
                        if (driver.getBit().equalsIgnoreCase("32")) {
                            osArch = "linux-i686.tar.bz2";
                        } else {
                            osArch = "linux-x86_64.tar.bz2";
                        }
                    }
                    if (driver.getPlatform().equalsIgnoreCase("windows")) {
                        osArch = "windows.zip";
                    }
                    if (driver.getPlatform().equalsIgnoreCase("mac")) {
                        osArch = "macosx.zip";
                    }
                    Files.copy(fakeDriverDir.resolve("phantomjs").resolve("phantomjs-2.1.1-" + osArch), downloadFilePath);
                }
                break;
                case "edgedriver":
                    Files.copy(fakeDriverDir.resolve("edgedriver").resolve("MicrosoftWebDriver.exe"), downloadFilePath);
                    break;
                case "operadriver": {
                    String osArch = "";
                    if (driver.getPlatform().equalsIgnoreCase("linux")) {
                        osArch = "linux" + driver.getBit();
                    }
                    if (driver.getPlatform().equalsIgnoreCase("windows")) {
                        osArch = "win" + driver.getBit();
                    }
                    if (driver.getPlatform().equalsIgnoreCase("mac")) {
                        osArch = "mac" + driver.getBit();
                    }
                    Files.copy(fakeDriverDir.resolve("operadriver").resolve("operadriver_" + osArch + ".zip"), downloadFilePath);
                }
                break;
                default:
                    throw new MojoExecutionException("missing fake-driver: " + driver);
            }
            Files.createFile(downloadDirectory.resolve("download.completed"));
            return downloadFilePath;
        }
        
    }
}
