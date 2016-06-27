package com.github.webdriverextensions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.attribute.FileTime;

import org.apache.maven.plugin.MojoExecutionException;

public class InstallDriversMojoTest extends AbstractInstallDriversMojoTest {

    public void test_that_configuration_with_explicit_driver_that_is_not_available_in_the_repository_fails_with_error_message_could_not_find_driver() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/driver_not_in_repositoy_pom.xml");


        try {
            // When
            mojo.execute();
            fail("should raise an exception");
        } catch (MojoExecutionException e) {
            // Then
            assertEquals("Could not find driver: " + mojo.drivers.get(0) + System.lineSeparator()
                    + System.lineSeparator()
                    + "in repository: " + mojo.repository, e.getMessage());
        }
    }

    public void test_that_driver_compressed_with_tar_bz2_is_supported() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/extract_tar_bz2_pom.xml");


        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("phantomjs-linux-32bit");
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_that_driver_compressed_with_zip_is_supported() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/extract_zip_pom.xml");


        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("phantomjs-windows-64bit.exe");
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_that_driver_compressed_with_gz_is_supported() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/extract_gz_pom.xml");


        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("geckodriver-linux-64bit");
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_that_skip_configuration_does_not_install_configured_drivers() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/skip_pom.xml");


        // When
        mojo.execute();

        // Then
        assertThat(mojo.installationDirectory.listFiles()).isNullOrEmpty();
    }

    public void test_that_installation_directory_configuration_installs_driver_into_custom_directory() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/installation_directory_pom.xml");


        // When
        mojo.execute();

        // Then
        assertTrue(mojo.installationDirectory.toString().endsWith("custom-installation-directory"));
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_that_configuration_with_custom_driver_not_in_repository_works() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/custom_driver_pom.xml");


        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("customdriver-windows-32bit");
        assertNumberOfInstalledDriverIs(1);
        File[] installedFiles = mojo.installationDirectory.listFiles();
        assertThat(installedFiles[0]).isDirectory();
        assertThat(installedFiles[0].listFiles()).hasSize(6);
        assertThat(installedFiles[1]).isFile();
    }

    public void test_that_configuration_with_custom_driver_not_in_repository_with_file_match_inside_works() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/custom_driver_file_match_inside_pom.xml");


        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("customdriver-filematchinside-windows-32bit.exe");
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_that_driver_already_downloaded_is_not_downloaded_again() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/a_driver_pom.xml");

        mojo.execute();
        FileTime creationTimeFirstInstallation = getDriverCreationTime("phantomjs-windows-64bit.exe");

        // When
        mojo.execute();

        // Then
        FileTime creationTimeAfterSecondInstallation = getDriverCreationTime("phantomjs-windows-64bit.exe");
        assertTrue("Driver already downloaded was downloaded again the second time the plugin ran",
                creationTimeFirstInstallation.compareTo(creationTimeAfterSecondInstallation) == 0);

    }

    public void test_that_driver_configuration_with_no_version_downloads_latest_drivers_from_remote_repository() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_version_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-linux-32bit");
        assertDriverIsInstalled("chromedriver-linux-64bit");
        assertDriverIsInstalled("chromedriver-mac-32bit");
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-64bit.exe");
        assertDriverIsInstalled("phantomjs-linux-32bit");
        assertDriverIsInstalled("phantomjs-linux-64bit");
        assertDriverIsInstalled("phantomjs-mac-64bit");
        assertDriverIsInstalled("phantomjs-windows-64bit.exe");
        assertDriverIsInstalled("geckodriver-windows-32bit.exe");
        assertDriverIsInstalled("geckodriver-mac-32bit");
        assertDriverIsInstalled("geckodriver-mac-64bit");
        assertDriverIsInstalled("geckodriver-linux-64bit");
        assertNumberOfInstalledDriverIs(14);
    }
}
