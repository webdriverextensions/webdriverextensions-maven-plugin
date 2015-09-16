package com.github.webdriverextensions;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;

import static com.github.webdriverextensions.Utils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class InstallDriversMojoTest extends AbstractMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void test_that_no_configuration_downloads_the_latest_driver_for_the_current_platform() throws Exception {
        // Given
        MavenProject project = getMavenProject("src/test/resources/test-mojo-no-configuration-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        FileUtils.deleteDirectory(installDriversMojo.installationDirectory);
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("## TEST: test_that_no_configuration_downloads_the_latest_driver_for_the_current_platform");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        installDriversMojo.execute();

        // Then
        if (isMac()) {
            TestUtils.assertDriverIsInstalled("chromedriver-mac-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit", installDriversMojo.installationDirectory);
        }
        if (isLinux()) {
            if (is64Bit()) {
                TestUtils.assertDriverIsInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
                TestUtils.assertDriverIsNotInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
                TestUtils.assertDriverIsNotInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            } else {
                TestUtils.assertDriverIsInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
                TestUtils.assertDriverIsNotInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
                TestUtils.assertDriverIsInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            }
            TestUtils.assertDriverIsNotInstalled("chromedriver-mac-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit", installDriversMojo.installationDirectory);
        }
        if (isWindows()) {
            TestUtils.assertDriverIsInstalled("chromedriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsInstalled("internetexplorerdriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-mac-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
        }
        FileUtils.deleteDirectory(installDriversMojo.installationDirectory);
    }

    public void test_that_driver_configuration_with_no_platform_downloads_the_drivers_only_for_the_current_platform() throws Exception {
        // Given
        MavenProject project = getMavenProject("src/test/resources/test-mojo-no-platform-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        FileUtils.deleteDirectory(installDriversMojo.installationDirectory);
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("## TEST: test_that_driver_configuration_with_no_platform_downloads_the_drivers_only_for_the_current_platform");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        installDriversMojo.execute();

        // Then
        if (isMac()) {
            TestUtils.assertDriverIsInstalled("chromedriver-mac-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit", installDriversMojo.installationDirectory);
        }
        if (isLinux()) {
            TestUtils.assertDriverIsInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-mac-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit", installDriversMojo.installationDirectory);
        }
        if (isWindows()) {
            TestUtils.assertDriverIsInstalled("chromedriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsInstalled("internetexplorerdriver-windows-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsInstalled("internetexplorerdriver-windows-64bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-mac-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("phantomjs-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-32bit", installDriversMojo.installationDirectory);
            TestUtils.assertDriverIsNotInstalled("chromedriver-linux-64bit", installDriversMojo.installationDirectory);
        }
        FileUtils.deleteDirectory(installDriversMojo.installationDirectory);
    }

    public void testConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("## TEST: testConfiguration");

        installDriversMojo.execute();
    }

    public void testConfigurationInstallAllLatestDrivers() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-install-all-latest-drivers-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("## TEST: testConfigurationInstallAllLatestDrivers");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        installDriversMojo.execute();
    }

    public void testRaiseErrorWhenDriverWasNotFoundInConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom_not_found_driver.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("");
        installDriversMojo.getLog().info("## TEST: testRaiseErrorWhenDriverWasNotFoundInConfiguration");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        try {
            installDriversMojo.execute();
            fail("should raise an exception");
        } catch (MojoExecutionException e) {
            assertEquals("could not found driver: {\"name\":\"phantooomjs\",\"platform\":\"linux\",\"bit\":\"32\",\"version\":\"1.9.7\"}", e.getMessage());
        }
    }


    public void testConfigurationExtractPhantomJSDriverFromTarBz2() throws Exception {
        File dir = new File("src/test/resources/target_phantomjs-extract-test");
        try {
            MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom_phantomjs-extract.xml");
            InstallDriversMojo mojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
            mojo.getLog().info("## TEST: testConfigurationExtractPhantomJSDriverFromTarBz2");
            mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

            mojo.execute();

            File[] files = dir.listFiles();
            assertThat(files).hasSize(2);
            assertThat(files[0]).isFile();
            assertThat(files[1]).isFile();
        } finally {
            FileUtils.deleteDirectory(dir);
        }
    }

    private MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }
}
