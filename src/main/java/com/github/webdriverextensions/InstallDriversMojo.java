package com.github.webdriverextensions;

import static com.github.webdriverextensions.ProxyUtils.getProxyFromSettings;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

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
    @Parameter(defaultValue = "https://raw.githubusercontent.com/webdriverextensions/webdriverextensions-maven-plugin-repository-2.0/master/repository.json")
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
     * <p/>
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
    List<Driver> drivers = new ArrayList<>();
    /**
     * Skips installation of drivers.
     */
    @Parameter(defaultValue = "false")
    boolean skip;

    /**
     * Keep downloaded files as local cache
     */
    @Parameter(defaultValue = "false")
    boolean keepDownloadedWebdrivers;

    File cacheDirectory = createCachePath();
    File tempDirectory = createTempPath();

    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping install-drivers goal execution");
        } else {
            Repository repository = Repository.load(repositoryUrl, getProxyFromSettings(settings, proxyId));
            getLog().info("Installation directory " + Utils.quote(installationDirectory));
            if (drivers.isEmpty()) {
                getLog().info("Installing latest drivers for current platform");
                drivers = repository.getLatestDrivers();
            } else {
                getLog().info("Installing drivers from configuration");
            }

            DriverDownloader driverDownloader = new DriverDownloader(settings, proxyId, getLog());
            DriverExtractor driverExtractor = new DriverExtractor(tempDirectory, keepDownloadedWebdrivers, getLog());
            DriverInstaller driverInstaller = new DriverInstaller(installationDirectory, getLog());

            cleanupTempDirectory();
            for (Driver _driver : drivers) {
                Driver driver = repository.enrichDriver(_driver);
                if (driver == null) {
                    continue;
                }
                getLog().info(driver.getId() + " version " + driver.getVersion());
                if (driverInstaller.needInstallation(driver)) {
                    File downloadPath = keepDownloadedWebdrivers ? Paths.get(cacheDirectory.getPath(), driver.getIdWithVersion()).toFile() : tempDirectory;
                    Path downloadLocation = driverDownloader.downloadFile(driver, downloadPath);
                    Path extractLocation = driverExtractor.extractDriver(driver, downloadLocation);
                    driverInstaller.install(driver, extractLocation);
                    cleanupTempDirectory();
                } else {
                    getLog().info("  Already installed");
                }
            }
        }
    }

    private void cleanupTempDirectory() throws MojoExecutionException {
        try {
            FileUtils.deleteDirectory(tempDirectory);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to delete temp directory:" + tempDirectory, ex);
        }
    }

    private static File createCachePath() {
        return Paths.get(getPluginWorkingDirectory(), "cache").toFile();
    }

    private static File createTempPath() {
        return new File(getPluginWorkingDirectory(), "temp");
    }

    private static String getPluginWorkingDirectory() {
        String systemTempDirectory = System.getProperty("java.io.tmpdir");
        return Paths.get(systemTempDirectory, "webdriverextensions-maven-plugin").toString();
    }

}
