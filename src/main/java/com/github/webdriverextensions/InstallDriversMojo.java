package com.github.webdriverextensions;

import static com.github.webdriverextensions.Utils.calculateChecksum;
import static com.github.webdriverextensions.Utils.deleteDirectory;
import static com.github.webdriverextensions.Utils.deleteFile;
import static com.github.webdriverextensions.Utils.directoryContainsSingleDirectory;
import static com.github.webdriverextensions.Utils.directoryContainsSingleFile;
import static com.github.webdriverextensions.Utils.downloadFile;
import static com.github.webdriverextensions.Utils.getProxyFromSettings;
import static com.github.webdriverextensions.Utils.makeExecutable;
import static com.github.webdriverextensions.Utils.moveAllFilesInDirectory;
import static com.github.webdriverextensions.Utils.moveDirectoryInDirectory;
import static com.github.webdriverextensions.Utils.moveFileInDirectory;
import static com.github.webdriverextensions.Utils.quote;
import static com.github.webdriverextensions.Utils.unzipFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import static org.codehaus.plexus.util.FileUtils.fileExists;

// TODO: refactor exception messages
@Mojo(name = "install-drivers", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class InstallDriversMojo extends AbstractMojo {

    @Component
    MavenProject project;

    @Parameter(defaultValue = "${settings}", readonly = true)
    Settings settings;

    /**
     * URL to where the repository file is located. The repository file is a
     * json file containing information of available drivers and their
     * locations, checksums, etc.
     */
    @Parameter(defaultValue = "https://raw.githubusercontent.com/webdriverextensions/webdriverextensions-maven-plugin-repository/master/repository.json")
    URL repositoryUrl;

    /**
     * The path to the directory where the drivers are going to be installed.
     */
    @Parameter(defaultValue = "${basedir}/drivers")
    File installationDirectory;

    /**
     * The id of the proxy to use if it is configured in settings.xml. If not provided the first
     * active proxy in settings.xml will be used.
     */
    @Parameter
    String proxyId;

    /**
     * List of drivers to install. Each driver has a name, platform, bit,
     * version, URL and checksum that can be provided.<br/>
     * <br/>
     * If no drivers are provided the latest drivers will be installed for the
     * running platform and the bit version will be chosen as if it has not
     * been provided (see rule below).<br/>
     * <br/>
     * If no platform is provided for a driver the platform will automatically
     * be set to the running platform.<br/>
     * <br/>
     * If no bit version is provided for a driver the bit will automatically
     * be set to 32 if running the plugin on a windows or mac platform. However
     * if running the plugin from a linux platform the bit will be determined
     * from the OS bit version.<br/>
     * <br/>
     * If the driver is not available in the repository the plugin does not know
     * from which URL to download the driver. In that case the URL should be
     * provided for the driver together with a checksum (to retrieve the
     * checksum run the plugin without providing a checksum once, the plugin
     * will then calculate and print the checksum for you). The default
     * repository with all available drivers can be found <a href="https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository/blob/master/repository.json">here</a>.<br/>
     * <br/>
     * <strong>Some Examples</strong><br/>
     * Installing all latest drivers<br/>
     * <pre>
     * &lt;drivers&gt;
     *   &lt;driver&gt;
     *     &lt;name&gt;chromedriver&lt;/name&gt;
     *     &lt;platform&gt;windows&lt;/platform&gt;
     *     &lt;bit&gt;32&lt;/bit&gt;
     *   &lt;/driver&gt;
     *   &lt;driver&gt;
     *     &lt;name&gt;chromedriver&lt;/name&gt;
     *     &lt;platform&gt;mac&lt;/platform&gt;
     *     &lt;bit&gt;32&lt;/bit&gt;
     *   &lt;/driver&gt;
     *   &lt;driver&gt;
     *     &lt;name&gt;chromedriver&lt;/name&gt;
     *     &lt;platform&gt;linux&lt;/platform&gt;
     *     &lt;bit&gt;32&lt;/bit&gt;
     *   &lt;/driver&gt;
     *   &lt;driver&gt;
     *     &lt;name&gt;chromedriver&lt;/name&gt;
     *     &lt;platform&gt;linux&lt;/platform&gt;
     *     &lt;bit&gt;64&lt;/bit&gt;
     *   &lt;/driver&gt;
     *   &lt;driver&gt;
     *     &lt;name&gt;internetexplorerdriver&lt;/name&gt;
     *     &lt;platform&gt;windows&lt;/platform&gt;
     *     &lt;bit&gt;32&lt;/bit&gt;
     *   &lt;/driver&gt;
     *   &lt;driver&gt;
     *     &lt;name&gt;internetexplorerdriver&lt;/name&gt;
     *     &lt;platform&gt;windows&lt;/platform&gt;
     *     &lt;bit&gt;64&lt;/bit&gt;
     *   &lt;/driver&gt;
     * &lt;/drivers&gt;
     * </pre>
     * <br/>
     *
     * Installing a driver not available in the repository, e.g. PhantomJS<br/>
     * <pre>
     * &lt;driver&gt;
     *   &lt;name&gt;phanthomjs&lt;/name&gt;
     *   &lt;platform&gt;mac&lt;/platform&gt;
     *   &lt;bit&gt;32&lt;/bit&gt;
     *   &lt;version&gt;1.9.7&lt;/version&gt;
     *   &lt;url&gt;http://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.7-macosx.zip&lt;/url&gt;
     *   &lt;checksum&gt;0f4a64db9327d19a387446d43bbf5186&lt;/checksum&gt;
     * &lt;/driver&gt;
     * </pre>
     */
    @Parameter
    List<Driver> drivers = new ArrayList<Driver>();
    /**
     * Skips installation of drivers.
     */
    @Parameter(defaultValue = "false")
    boolean skip;

    String tempDirectory;
    Repository repository;

    public void execute() throws MojoExecutionException {

        tempDirectory = project.getBuild().getDirectory() + "/temp/com/github/webdriverextensions";

        if (skip) {
            getLog().info("Skipping install-drivers goal execution");
        } else {
            repository = Repository.load(repositoryUrl, getProxyFromSettings(settings, proxyId));
            getLog().info("Installation directory " + quote(installationDirectory));
            if (drivers.isEmpty()) {
                getLog().info("Installing latest drivers for current platform");
                drivers = repository.getLatestDrivers();
            } else {
                getLog().info("Installing drivers from configuration");
            }
            for (Driver _driver : drivers) {
                Driver driver = repository.getDriver(_driver);
                if (driver == null) {
                    throw new MojoExecutionException("could not found driver: " + _driver);
                }
                getLog().info(driver.getId() + " version " + driver.getVersion());
                if (driverIsNotInstalled(driver) || driverVersionIsNew(driver)) {
                    cleanup();
                    downloadDriver(driver);

                    String fileMatchInside = driver.getFileMatchInside();
                    if (null != fileMatchInside && !fileMatchInside.isEmpty()) {
                        extractFileFromInside(fileMatchInside, tempDirectory + "/" + driver.getUrlFileName());
                    }

                    if (downloadedDriverIsZipped(driver)) {
                        unzipDriver(driver);
                    }
                    if (StringUtils.isBlank(driver.getChecksum())) {
                        printChecksumMissingWarning(driver);
                    } else {
                        verifyChecksum(driver);
                        installDriver(driver);
                    }
                    cleanup();
                } else {
                    getLog().info("  Already installed");
                }
            }
        }
    }

    boolean driverIsNotInstalled(Driver driver) throws MojoExecutionException {
        return !fileExists(installationDirectory + "/" + driver.getFileName());
    }

    boolean driverVersionIsNew(Driver driver) throws MojoExecutionException {
        String checksum = calculateChecksum(installationDirectory + "/" + driver.getFileName());
        return !checksum.equals(driver.getChecksum());
    }

    void printChecksumMissingWarning(Driver driver) throws MojoExecutionException {
        String checksum = calculateChecksum(tempDirectory);
        getLog().warn("Skipped " + driver.getId() + " version " + driver.getVersion() + ", please set checksum to " + checksum + " to install the driver");
    }

    void downloadDriver(Driver driver) throws MojoExecutionException {
        getLog().info("  Downloading");
        downloadFile(driver.getUrl(), tempDirectory + "/" + driver.getUrlFileName(), getLog(), getProxyFromSettings(settings, proxyId));
    }

    private void extractFileFromInside(String fileMatchInside, String downloadFileLocation) throws MojoExecutionException {
        Path path = Paths.get(downloadFileLocation);
        String tempLocation = downloadFileLocation + ".temp";
        File tempFile = new File(tempLocation);
        Pattern pattern = Pattern.compile(fileMatchInside);

        try {
            String contentType = Files.probeContentType(path);
            switch (contentType) {
                case "application/x-bzip":
                    BZip2CompressorInputStream bZip2CompressorInputStream = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(downloadFileLocation)));

                    FileUtils.copyInputStreamToFile(bZip2CompressorInputStream, tempFile);
                    if (!tempFile.renameTo(new File(downloadFileLocation))) {
                        throw new MojoExecutionException("renaming failed, from " + tempLocation + " -> " + downloadFileLocation);
                    }
                    extractFileFromInside(fileMatchInside, downloadFileLocation);
                    break;
                case "application/x-tar":
                    TarArchiveInputStream tarStream = null;
                    try {
                        tarStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", new BufferedInputStream(new FileInputStream(downloadFileLocation)));
                        TarArchiveEntry entry;

                        int matchCount = 0;
                        while ((entry = (TarArchiveEntry) tarStream.getNextEntry()) != null) {
                            boolean matches = pattern.matcher(entry.getName()).matches();

                            if (matches) {
                                if (!entry.isFile()) {
                                    throw new MojoExecutionException("can only handle files");
                                }

                                matchCount++;
                                if (matchCount > 1) {
                                    throw new MojoExecutionException("at least two entries matches, this seems to be wrong");
                                }

                                getLog().debug("entry " + entry.getName() + " matches '" + fileMatchInside + "'");
                                IOUtils.copy(tarStream, new BufferedOutputStream(new FileOutputStream(tempFile)));
                                if (!tempFile.renameTo(new File(downloadFileLocation))) {
                                    throw new MojoExecutionException("renaming failed, from " + tempLocation + " -> " + downloadFileLocation);
                                }
                            }
                        }
                    } catch (ArchiveException e) {
                        throw new MojoExecutionException(e.getMessage(), e);
                    } finally {
                        if (tarStream != null) {
                            tarStream.close();
                        }
                    }
                    break;
                case "application/x-executable":
                    getLog().debug("finally extracted");
                    break;
                default:
                    throw new MojoExecutionException("unhandled content-type:" + contentType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean downloadedDriverIsZipped(Driver driver) {
        return driver.getUrl().toLowerCase().endsWith(".zip");
    }

    void unzipDriver(Driver driver) throws MojoExecutionException {
        getLog().info("  Unzipping");
        unzipFile(tempDirectory + "/" + driver.getUrlFileName(), tempDirectory);
        deleteFile(tempDirectory + "/" + driver.getUrlFileName());
    }

    void verifyChecksum(Driver driver) throws MojoExecutionException {
        getLog().info("  Verifying checksum");
        String checksum = calculateChecksum(tempDirectory);
        if (!checksum.equals(driver.getChecksum())) {
            throw new MojoExecutionException("Error checksum is " + quote(checksum) + " for downloaded driver, when it should be "
                    + quote(driver.getChecksum()));
        }
    }

    void installDriver(Driver driver) throws MojoExecutionException {
        getLog().info("  Installing");
        if (directoryContainsSingleDirectory(tempDirectory)) {
            moveDirectoryInDirectory(tempDirectory, installationDirectory + "/" + driver.getId());
        } else if (directoryContainsSingleFile(tempDirectory)) {
            moveFileInDirectory(tempDirectory, installationDirectory + "/" + driver.getFileName());
            makeExecutable(installationDirectory + "/" + driver.getFileName());
        } else {
            moveAllFilesInDirectory(tempDirectory, installationDirectory + "/" + driver.getId());
        }
    }

    void cleanup() throws MojoExecutionException {
        getLog().debug("  Cleaning up temp directory: " + tempDirectory);
        deleteDirectory(tempDirectory);
    }

}
