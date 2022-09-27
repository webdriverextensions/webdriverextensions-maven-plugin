package com.github.webdriverextensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.stream.Collectors;
import java.util.logging.Level;
import lombok.extern.java.Log;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.github.webdriverextensions.Utils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Log
public abstract class AbstractInstallDriversMojoTest extends AbstractMojoTestCase {

    private InstallDriversMojo mojo;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempFolder.create();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tempFolder.delete();
    }

    protected MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }

    protected InstallDriversMojo getMojo(String pomPath) throws Exception {
        MavenProject project = getMavenProject(pomPath);
        mojo = Mockito.spy((InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers"));

        logTestName();

        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository-3.0.json");
        mojo.installationDirectory = tempFolder.newFolder();
        mojo.pluginWorkingDirectory = tempFolder.newFolder();
        DriverDownloader dlMock = Mockito.mock(DriverDownloader.class);
        when(dlMock.downloadFile(any(Driver.class), any(Path.class))).thenAnswer(new DownloadAnswer());
        doReturn(dlMock).when(mojo).createDownloader();

        return mojo;
    }

    private void logTestName() {
        log.log(Level.INFO, () -> {
            StackTraceElement[] stackTrace = new Exception().getStackTrace();
            return String.format(
                    "%n%n## TEST: %s.%s on platform %S(%s) %sBIT",
                    stackTrace[4].getClassName(),
                    stackTrace[4].getMethodName(),
                    detectPlatform(),
                    detectArch(),
                    detectBits()
            );
        });
    }

    FileTime getDriverCreationTime(String driver) throws IOException {
        for (File file : mojo.installationDirectory.listFiles()) {
            if (file.getName().equals(driver)) {
                return Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime();
            }
        }
        fail("Did not find driver creation time for driver " + driver + " since driver file or folder is not installed"
             + System.lineSeparator() + directoryToString(mojo.installationDirectory.toPath()));
        return null;
    }

    void assertDriverIsInstalled(String driverFileName) {
        assertDriverIsInstalled(driverFileName, null, null);
    }

    void assertDriverIsInstalled(String driverFileName, String version) {
        assertDriverIsInstalled(driverFileName, version, null);
    }

    void assertDriverIsInstalled(String driverFileName, Architecture arch) {
        assertDriverIsInstalled(driverFileName, null, arch);
    }

    void assertDriverIsInstalled(String driverFileName, String version, Architecture arch) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        String versionFilename = driverFileName.replace(".exe", "") + ".version";
        for (File file : mojo.installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(versionFilename)) {
                foundDriverVersionFile = true;
                if (version != null || arch != null) {
                    try {
                        String versionFileString = Files.lines(file.toPath()).collect(Collectors.joining());
                        Driver driverFromVersionfile = Driver.fromJson(versionFileString);
                        if (version != null && !driverFromVersionfile.getVersion().equalsIgnoreCase(version)) {
                            fail("Version " + quote(version) + " was not found in version file, version file content: " + versionFileString);
                        }
                        if (arch != null && driverFromVersionfile.getArchitecture() != arch) {
                            fail("arch '" + arch + "' was not found in version file, version file content: " + versionFileString);
                        }
                    } catch (IOException e) {
                        fail("Failed to read version file " + quote(versionFilename));
                    }
                }
            }
        }
        if (!foundDriverFile) {
            fail("Driver with file name " + quote(driverFileName) + " was not found in the installation directory"
                 + System.lineSeparator() + directoryToString(mojo.installationDirectory.toPath()));
        }
        if (!foundDriverVersionFile) {
            fail("Driver version file with file name " + quote(versionFilename) + " was not found in the installation directory"
                 + System.lineSeparator() + directoryToString(mojo.installationDirectory.toPath()));
        }
    }

    void assertNumberOfInstalledDriverIs(int numberOfDrivers) {
        int length = mojo.installationDirectory.listFiles().length;
        if (length != numberOfDrivers * 2) {
            fail("Number of drivers installed is not " + numberOfDrivers + ", it is " + (length / 2)
                 + System.lineSeparator() + directoryToString(mojo.installationDirectory.toPath()));
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

    void fakeArch(Architecture arch) {
        System.setProperty(FAKED_ARCH_PROPERTY_KEY, arch.toString());
    }

    void resetFakes() {
        System.clearProperty(FAKED_OS_NAME_PROPERTY_KEY);
        System.clearProperty(FAKED_BIT_PROPERTY_KEY);
        System.clearProperty(FAKED_ARCH_PROPERTY_KEY);
    }
    
    public static void fail(String message) {
        Assert.fail(String.format("[%S(%S) %sBIT] " + message, detectPlatform(), detectArch(), detectBits()));
    }

    private class DownloadAnswer implements Answer<Path> {

        @Override
        public Path answer(InvocationOnMock invocation) throws Throwable {
            Driver driver = invocation.getArgument(0);
            Path baseDownloadDirectory = invocation.getArgument(1);
            Path downloadDirectory = baseDownloadDirectory.resolve(driver.getDriverDownloadDirectoryName());
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
