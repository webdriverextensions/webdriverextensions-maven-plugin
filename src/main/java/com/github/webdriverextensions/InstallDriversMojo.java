package com.github.webdriverextensions;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.UncheckedException;
import org.jooq.lambda.tuple.Tuple;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.webdriverextensions.ProxyUtils.getProxyFromSettings;
import static com.github.webdriverextensions.Utils.quote;

/**
 * Download and install WebDriver drivers.
 */
@Mojo(name = "install-drivers", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresOnline = true)
public class InstallDriversMojo extends AbstractMojo {

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    Settings settings;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    MavenSession session;

    /**
     * URL to where the repository file is located. The repository file is a
     * json file containing information of available drivers and their locations
     * etc. Supported protocols are: http, https and file. The content of the
     * file must validate against
     * <a href="https://raw.githubusercontent.com/webdriverextensions/webdriverextensions-maven-plugin-repository/master/drivers-schema.json">the
     * drivers repository JSON schema</a>.
     */
    @Parameter(defaultValue = "https://raw.githubusercontent.com/webdriverextensions/webdriverextensions-maven-plugin-repository/master/repository-3.0.json", property = "webdriverextensions.repoUrl")
    URL repositoryUrl;

    /**
     * The path to the directory where the drivers are going to be installed.
     */
    @Parameter(defaultValue = "${basedir}/drivers", property = "webdriverextensions.installDirectory")
    File installationDirectory;

    /**
     * The id of the proxy to use if it is configured in settings.xml. If not provided the first
     * active proxy in settings.xml will be used.
     * 
     * @since 1.1.0
     */
    @Parameter(property = "webdriverextensions.proxyId")
    String proxyId;

    /**
     * List of drivers to install. Each driver has a name, platform, bit,
     * version and URL that can be provided.<br/>
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
     * provided for the driver. The default
     * repository with all available drivers can be found <a href="https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository/blob/master/repository-3.0.json">here</a>.<br/>
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
     * &lt;/driver&gt;
     * </pre>
     */
    @Parameter
    List<Driver> drivers = new ArrayList<>();

    /**
     * Skips installation of drivers. Execution will also be skipped if one of
     * the following properties is <code>true</code>:
     * <ul>
     * <li>skipTests</li>
     * <li>skipITs</li>
     * <li>maven.test.skip</li>
     * </ul>
     */
    @Parameter(defaultValue = "false", property = "webdriverextensions.skip")
    boolean skip;
    
    /**
     * Do not skip execution even if one of the skip-properties (see
     * {@link #skip}) is <code>true</code>. This is useful for downloading
     * drivers even when test execution is skipped.
     *
     * @since 3.3.0
     */
    @Parameter(defaultValue = "false", property = "webdriverextensions.skipIgnore")
    boolean skipIgnore;

    /**
     * Determines the timeout in seconds until arrival of a response from the
     * download host.<br/>
     * A timeout value of zero is interpreted as an infinite timeout.
     * 
     * @since 3.3.0
     */
    @Parameter(defaultValue = "1800", property = "webdriverextensions.download.responseTimeout")
    int downloadResponseTimeout;

    /**
     * Determines the timeout in seconds until a new connection is fully established.<br/>
     * A timeout value of zero is interpreted as an infinite timeout.
     * 
     * @since 3.3.0
     */
    @Parameter(defaultValue = "30", property = "webdriverextensions.download.connectTimeout")
    int downloadConnectTimeout;

    /**
     * Number of retry attempts to download a driver.<br/>
     * A value of zero means no retries.<br/>
     * Retriable HTTP status codes are 429 and 503.<br/>
     * Retries may also happen for the following exceptions:
     * <ul>
     * <li>InterruptedIOException</li>
     * <li>UnknownHostException</li>
     * <li>ConnectException</li>
     * <li>ConnectionClosedException</li>
     * <li>NoRouteToHostException</li>
     * <li>SSLException</li>
     * </ul>
     * 
     * @since 3.3.0
     */
    @Parameter(defaultValue = "3", property = "webdriverextensions.download.maxRetries")
    int downloadMaxRetries;

    /**
     * retry interval in seconds between subsequent retries 
     * 
     * @since 3.3.0
     */
    @Parameter(defaultValue = "3", property = "webdriverextensions.download.retryDelay")
    int downloadRetryDelay;

    /**
     * Keep downloaded files as local cache.<br/>
     * <b>If set to <code>true</code>, one should also provide a known and
     * non-random value for <code>pluginWorkingDirectory</code>!</b>
     * 
     * @since 2.0.0
     */
    @Parameter(defaultValue = "false", property = "webdriverextensions.download.keepFiles")
    boolean keepDownloadedWebdrivers;

    /**
     * The working directory where downloaded drivers will be saved until they
     * are moved to <code>installationDirectory</code>.<br/>
     * <b>This defaults to a random temporary directory</b> prefixed with
     * <q>webdriverextensions-maven-plugin</q> below the default temporary-file
     * directory (<code>java.io.tmpdir</code>).
     * 
     * @since 3.3.0
     */
    @Parameter(property = "webdriverextensions.workingDirectory")
    File pluginWorkingDirectory;
  
    /**
     * Sets
     * <a href="https://www.selenium.dev/documentation/getting_started/installing_browser_drivers/">the
     * webdriver properties</a> with the path to the downloaded/installed
     * drivers.<br/>
     * <b>This setting should not be used if these properties are set
     * elsewhere</b>
     * (e.g.
     * <a href="https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#systemPropertyVariables">with
     * "systemPropertyVariables"</a>) because they will be overwritten!<br/>
     * For custom defined drivers: the driver name should start with one of the
     * known prefixes ("chrome", "chromium", "edge", "firefox", "gecho",
     * "opera", "internetexplorer"), otherwise the plugin will not know which
     * property to set.
     *
     * @since 3.3.0
     */
    @Parameter(defaultValue = "false", property = "webdriverextensions.setWebdriverPath")
    boolean setWebdriverPath;
  
    Path downloadDirectory;
    Path tempDirectory;
    Repository repository;

    public InstallDriversMojo() {
    }

    private void setupDirectories() throws MojoExecutionException {
        try {
            if (pluginWorkingDirectory == null) {
                pluginWorkingDirectory = Files.createTempDirectory("webdriverextensions-maven-plugin").toFile();
            }
            if (!Files.isDirectory(pluginWorkingDirectory.toPath())) {
                Files.createDirectories(pluginWorkingDirectory.toPath());
            }
            downloadDirectory = pluginWorkingDirectory.toPath().resolve("downloads");
            tempDirectory = Files.createTempDirectory(pluginWorkingDirectory.toPath(), "temp");
        } catch (IOException e) {
            throw new MojoExecutionException("error while creating folders", e);
        }
    }

    private boolean doSkip() {
        if (skipIgnore) {
            return false;
        } else {
            if (skip) {
                return true;
            } else {
                return Stream.of("skipTests", "skipITs", "maven.test.skip")
                        .map(Boolean::getBoolean)
                        .anyMatch(Boolean.TRUE::equals);
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (doSkip()) {
            getLog().info("Skipping install-drivers goal execution");
            return;
        }

        repository = Repository.load(repositoryUrl, getProxyFromSettings(settings, proxyId));
        getLog().info("Installation directory " + quote(installationDirectory.toPath()));
        if (drivers.isEmpty()) {
            getLog().info("Installing latest drivers for current platform");
            drivers = repository.getLatestDrivers();
        } else {
            getLog().info("Installing drivers from configuration");
        }

        
        if (keepDownloadedWebdrivers && pluginWorkingDirectory == null) {
            getLog().warn("keepDownloadedWebdrivers is true but pluginWorkingDirectory is not set! Please configure pluginWorkingDirectory as well.");
            keepDownloadedWebdrivers = false;
        }
        setupDirectories();
        performInstallation();
        if (keepDownloadedWebdrivers) {
            cleanupTempDirectory();
        } else {
            cleanupWorkingDirectory();
        }
    }

    private void performInstallation() throws MojoExecutionException {
        final DriverExtractor driverExtractor = new DriverExtractor(this);
        final DriverInstaller driverInstaller = new DriverInstaller(this);

        try (final DriverDownloader driverDownloader = getDownloader()) {
            drivers.stream()
                    .map(Unchecked.function(repository::enrichDriver))
                    .filter(Objects::nonNull)
                    // if driver is already installed, just set webdriver property
                    .peek(driverInstaller::setDriverPathPropertyIfInstalled)
                    // proceed with drivers that need installation
                    .filter(driverInstaller::needInstallation)
                    .peek(driver -> getLog().info(driver.getId() + " version " + driver.getVersion()))
                    // download
                    .map(Unchecked.function(driver -> {
                        Path downloadPath = downloadDirectory.resolve(driver.getDriverDownloadDirectoryName());
                        Path downloadLocation = driverDownloader.downloadFile(driver, downloadPath);
                        return Tuple.tuple(driver, downloadLocation);
                    }))
                    // extract
                    .map(Unchecked.function(t -> {
                        Path extractLocation = driverExtractor.extractDriver(t.v1, t.v2);
                        return Tuple.tuple(t.v1, extractLocation);
                    }))
                    // and finally install the driver
                    .forEach(Unchecked.consumer(t -> {
                        driverInstaller.install(t.v1, t.v2);
                    }));
        } catch (IOException ex) {
            // ignored. close operation of downloader
        } catch (UncheckedException ex) {
            if (ex.getCause() instanceof MojoExecutionException) {
                throw (MojoExecutionException) ex.getCause();
            }
            throw new InstallDriversMojoExecutionException(ex.getMessage(), ex);
        }
    }

    DriverDownloader getDownloader() throws MojoExecutionException {
        return new DriverDownloader(this);
    }

    private void cleanupWorkingDirectory() throws MojoExecutionException {
        try {
            FileUtils.deleteDirectory(pluginWorkingDirectory);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to delete plugin working directory:" + System.lineSeparator()
                    + Utils.directoryToString(pluginWorkingDirectory.toPath()), e);
        }
    }

    private void cleanupTempDirectory() throws MojoExecutionException {
        try {
            FileUtils.deleteDirectory(tempDirectory.toFile());
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to delete temp directory:" + System.lineSeparator()
                    + Utils.directoryToString(tempDirectory), e);
        }
    }

}
