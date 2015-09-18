package com.github.webdriverextensions;

public class InstallDriversOnWindows32BitMachineMojoTest extends AbstractInstallDriverMojoTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fakePlatformToBeWindows();
        fakeBitToBe32();
    }

    public void test_that_no_configuration_downloads_the_latest_driver_for_the_current_platform() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_configuration_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit.exe");
        assertDriverIsNotInstalled("chromedriver-mac-32bit");
        assertDriverIsNotInstalled("phantomjs-linux-32bit");
        assertDriverIsNotInstalled("chromedriver-linux-32bit");
        assertDriverIsNotInstalled("chromedriver-linux-64bit");
    }

    public void test_that_driver_configuration_with_no_platform_downloads_the_driver_only_for_the_current_platform() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_platform_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-64bit.exe");
        assertDriverIsNotInstalled("chromedriver-mac-32bit");
        assertDriverIsNotInstalled("phantomjs-linux-32bit");
        assertDriverIsNotInstalled("chromedriver-linux-32bit");
        assertDriverIsNotInstalled("chromedriver-linux-64bit");
    }

    public void test_that_driver_configuration_with_no_bit_downloads_the_driver_only_for_the_current_bit() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_bit_pom.xml", "install-drivers");
        mojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-mac-32bit");
        assertDriverIsInstalled("chromedriver-linux-32bit");
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("phantomjs-linux-32bit");
        assertDriverIsNotInstalled("chromedriver-linux-64bit");
        assertDriverIsNotInstalled("internetexplorerdriver-windows-64bit.exe");
    }
}
