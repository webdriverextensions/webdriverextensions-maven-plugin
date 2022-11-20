package com.github.webdriverextensions;

public class InstallDriversOnMacAARCH64MachineMojoTest extends AbstractInstallDriversMojoTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fakePlatformToBeMac();
        fakeArch(Architecture.ARM64);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        resetFakes();
    }

    public void test_that_no_configuration_downloads_the_latest_driver_for_the_current_platform() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_configuration_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-mac-64bit", Architecture.ARM64);
        assertDriverIsInstalled("chromedriver-beta-mac-64bit", Architecture.UNKNOWN);
        assertDriverIsInstalled("phantomjs-mac-64bit");
        assertDriverIsInstalled("geckodriver-mac-64bit");
        assertDriverIsInstalled("operadriver-mac-64bit");
        assertNumberOfInstalledDriverIs(5);
    }

    public void test_that_driver_configuration_with_no_platform_downloads_the_driver_only_for_the_current_platform() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_platform_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-mac-32bit", Architecture.UNKNOWN);
        assertDriverIsInstalled("phantomjs-mac-64bit");
        assertDriverIsInstalled("geckodriver-mac-64bit");
        assertDriverIsInstalled("operadriver-mac-64bit");
        assertNumberOfInstalledDriverIs(4);
    }

    public void test_that_driver_configuration_with_no_bit_downloads_the_driver_only_for_the_current_bit() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_bit_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("chromedriver-mac-32bit", Architecture.UNKNOWN);
        assertDriverIsInstalled("chromedriver-linux-64bit");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("phantomjs-linux-64bit");
        assertDriverIsInstalled("geckodriver-windows-64bit.exe");
        assertDriverIsInstalled("geckodriver-linux-64bit");
        assertDriverIsInstalled("geckodriver-mac-64bit");
        assertDriverIsInstalled("edgedriver-windows-64bit.exe");
        assertDriverIsInstalled("operadriver-windows-64bit.exe");
        assertDriverIsInstalled("operadriver-linux-64bit");
        assertDriverIsInstalled("operadriver-mac-64bit");
        assertNumberOfInstalledDriverIs(12);
    }

    public void test_that_driver_configuration_with_no_version_downloads_latest_drivers() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_version_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-linux-32bit");
        assertDriverIsInstalled("chromedriver-linux-64bit");
        assertDriverIsInstalled("chromedriver-mac-32bit", Architecture.UNKNOWN);
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-64bit.exe");
        assertDriverIsInstalled("phantomjs-linux-32bit");
        assertDriverIsInstalled("phantomjs-linux-64bit");
        assertDriverIsInstalled("phantomjs-mac-64bit");
        assertDriverIsInstalled("phantomjs-windows-64bit.exe");
        assertDriverIsInstalled("geckodriver-windows-64bit.exe");
        assertDriverIsInstalled("geckodriver-mac-64bit");
        assertDriverIsInstalled("geckodriver-linux-64bit");
        assertDriverIsInstalled("edgedriver-windows-64bit.exe");
//        assertDriverIsInstalled("operadriver-windows-32bit.exe");
//        assertDriverIsInstalled("operadriver-windows-64bit.exe");
        assertDriverIsInstalled("operadriver-mac-64bit");
        assertDriverIsInstalled("operadriver-linux-32bit");
        assertDriverIsInstalled("operadriver-linux-64bit");
        assertNumberOfInstalledDriverIs(19);
    }

}
