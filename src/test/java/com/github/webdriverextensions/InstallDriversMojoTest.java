package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallDriversMojoTest extends AbstractInstallDriverMojoTest {

    public void test_raise_error_when_driver_was_not_found_in_repository() throws Exception {
        InstallDriversMojo mojo = getMojo("src/test/resources/driver_not_found_in_repositoy_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        try {
            mojo.execute();
            fail("should raise an exception");
        } catch (MojoExecutionException e) {
            assertEquals("Could not find driver: {\"name\":\"phantooomjs\",\"platform\":\"linux\",\"bit\":\"32\",\"version\":\"1.9.7\"}", e.getMessage());
        }
    }

    public void test_configuration_extract_phantom_j_s_driver_from_tar_bz2() throws Exception {
        InstallDriversMojo mojo = getMojo("src/test/resources/phantomjs_extract_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        mojo.execute();

        assertDriverIsInstalled("phantomjs-linux-32bit");
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_configuration_with_custom_driver_not_in_repository() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/custom_driver_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("customdriver-windows-32bit");
        assertNumberOfInstalledDriverIs(1);
        File[] installedFiles = installationDirectory.listFiles();
        assertThat(installedFiles[0]).isDirectory();
        assertThat(installedFiles[0].listFiles()).hasSize(6);
        assertThat(installedFiles[1]).isFile();
    }

    public void test_configuration_with_custon_driver_not_in_repository_with_file_match_inside() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/custom_driver_file_match_inside_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("customdriver-filematchinside-windows-32bit.exe");
        assertNumberOfInstalledDriverIs(1);
    }
}
