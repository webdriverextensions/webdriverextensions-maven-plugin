package com.github.webdriverextensions;

import static com.github.webdriverextensions.Utils.calculateChecksum;
import static com.github.webdriverextensions.Utils.directoryContainsSingleDirectory;
import static com.github.webdriverextensions.Utils.directoryContainsSingleFile;
import static com.github.webdriverextensions.Utils.downloadFile;
import static com.github.webdriverextensions.Utils.getProxyFromSettings;
import static com.github.webdriverextensions.Utils.makeExecutable;
import static com.github.webdriverextensions.Utils.moveAllFilesInDirectory;
import static com.github.webdriverextensions.Utils.moveDirectoryInDirectory;
import static com.github.webdriverextensions.Utils.moveFileInDirectory;
import static com.github.webdriverextensions.Utils.quote;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
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

    private String tempDirectory = createTempPath();
    private Repository repository;

    public void execute() throws MojoExecutionException {

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
//                    cleanup();
                    Path downloadLocation = downloadDriver(driver);
                    extractDriver(driver,downloadLocation);

//                    if (StringUtils.isBlank(driver.getChecksum())) {
//                        printChecksumMissingWarning(driver);
//                    } else {
//                        verifyChecksum(driver);
//                        installDriver(driver);
//                    }
//                    cleanup();
                } else {
                    getLog().info("  Already installed");
                }
            }
        }
    }

    private static String createTempPath() {
        String systemTemporaryDestination = System.getProperty("java.io.tmpdir");
        String folderIdentifier = InstallDriversMojo.class.getSimpleName();
        return Paths.get(systemTemporaryDestination,folderIdentifier).toString();
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

    Path downloadDriver(Driver driver) throws MojoExecutionException {
        Proxy proxyFromSettings = getProxyFromSettings(settings, proxyId);
        return downloadFile(driver, tempDirectory, getLog(), proxyFromSettings);
    }

    private void extractDriver(Driver driver, Path path) throws MojoExecutionException {

        String filename = path.toString();
        String filextension = FilenameUtils.getExtension(filename);

        String extractedFilename = FilenameUtils.getName(path.toString()).replaceFirst("\\."+ filextension+"$","");
        Path extractPath = Paths.get(path.getParent().toString(), extractedFilename);

        getLog().debug("handling type:" + filextension + "(" + filename + ")");

        try {
            switch (filextension) {
                case "bz2":
                    try (FileInputStream fin = new FileInputStream(path.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (BZip2CompressorInputStream input = new BZip2CompressorInputStream(bin)) {
                                FileUtils.copyInputStreamToFile(input, extractPath.toFile());
                                extractDriver(driver, extractPath);
                            }
                        }
                    }
                    break;
                case "tar":
                case "zip":
                    try (FileInputStream fin = new FileInputStream(path.toFile())) {
                        try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                            try (ArchiveInputStream aiStream = new ArchiveStreamFactory().createArchiveInputStream(filextension, bin)) {

                                Path extractToDirectory = Paths.get(tempDirectory, driver.getId());
                                if (extractToDirectory.toFile().exists()) {
                                    FileUtils.deleteDirectory(extractToDirectory.toFile());
                                }
                                extractToDirectory.toFile().mkdirs();

                                ArchiveEntry entry;
                                while ((entry = aiStream.getNextEntry()) != null) {
                                    String name = entry.getName();
                                    if (entry.isDirectory()) {
                                        File directory = new File(extractToDirectory.toFile(), name);
                                        if (!directory.mkdirs()) {
                                            throw new MojoExecutionException("failed to create " + directory);
                                        }
                                    } else {
                                        File file = null;
                                        if (entry instanceof TarArchiveEntry) {
                                            TarArchiveEntry archiveEntry = (TarArchiveEntry) entry;
                                            if (archiveEntry.isFile()) {
                                                file = new File(extractToDirectory.toFile(), name);
                                            }
                                        } else if (entry instanceof ZipArchiveEntry) {
                                            ZipArchiveEntry archiveEntry = (ZipArchiveEntry) entry;
                                            if (!archiveEntry.isUnixSymlink()) {
                                                file = new File(extractToDirectory.toFile(), name);
                                            }
                                        }

                                        if (file != null) {
                                            try (OutputStream out = new FileOutputStream(file)) {
                                                IOUtils.copy(aiStream, out);
                                            }
                                        }
                                    }
                                }
                            } catch (ArchiveException e) {
                                throw new MojoExecutionException(e.getMessage(), e);
                            }
                        }
                    }
                    break;
                default:
                    throw new MojoExecutionException("unhandled type:" + filextension);}
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(),e);
        }
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
        try {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(tempDirectory);
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when deleting directory " + quote(tempDirectory), ex);
        }
    }
}
