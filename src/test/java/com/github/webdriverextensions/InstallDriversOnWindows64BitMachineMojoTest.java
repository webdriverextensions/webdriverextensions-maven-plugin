package com.github.webdriverextensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class InstallDriversOnWindows64BitMachineMojoTest extends AbstractInstallDriversMojoTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fakePlatformToBeWindows();
        fakeBitToBe64();
    }

    public void test_that_no_configuration_downloads_the_latest_driver_for_the_current_platform() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_configuration_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("chromedriver-beta-windows-32bit.exe");
        assertDriverIsInstalled("phantomjs-windows-64bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("geckodriver-windows-64bit.exe");
        assertDriverIsInstalled("edgedriver-windows-64bit.exe");
//        assertDriverIsInstalled("operadriver-windows-64bit.exe");
        assertNumberOfInstalledDriverIs(7);

        assertThat(mojo.session.getUserProperties())
                .doesNotContainKeys("webdriver.gecko.driver", "webdriver.chrome.driver", "webdriver.ie.driver", "webdriver.edge.driver", "webdriver.opera.driver");
    }

    public void test_that_setWebdriverPath_sets_webdriver_properties() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/setWebdriverPath.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("phantomjs-windows-64bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("geckodriver-windows-64bit.exe");
        assertDriverIsInstalled("edgedriver-windows-64bit.exe");
        assertNumberOfInstalledDriverIs(7);

        assertThat(mojo.session.getUserProperties())
                .contains(entry("webdriver.gecko.driver", mojo.installationDirectory.toPath().resolve("geckodriver-windows-64bit.exe").toString()))
                .contains(entry("webdriver.ie.driver", mojo.installationDirectory.toPath().resolve("internetexplorerdriver-windows-32bit.exe").toString()))
                .contains(entry("webdriver.edge.driver", mojo.installationDirectory.toPath().resolve("edgedriver-windows-64bit.exe").toString()))
                .containsKey("webdriver.chrome.driver");
    }

    public void test_that_driver_configuration_with_no_platform_downloads_the_driver_only_for_the_current_platform() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_platform_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-64bit.exe");
        assertDriverIsInstalled("phantomjs-windows-64bit.exe");
        assertDriverIsInstalled("geckodriver-windows-64bit.exe");
        assertDriverIsInstalled("edgedriver-windows-64bit.exe");
        assertDriverIsInstalled("operadriver-windows-32bit.exe");
        assertDriverIsInstalled("operadriver-windows-64bit.exe");
        assertNumberOfInstalledDriverIs(8);
    }

    public void test_that_driver_configuration_with_no_bit_downloads_the_driver_only_for_the_current_bit() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/no_bit_pom.xml");

        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-mac-32bit");
        assertDriverIsInstalled("chromedriver-linux-64bit");
        assertDriverIsInstalled("chromedriver-windows-32bit.exe");
        assertDriverIsInstalled("internetexplorerdriver-windows-32bit.exe");
        assertDriverIsInstalled("phantomjs-linux-64bit");
        assertDriverIsInstalled("geckodriver-windows-64bit.exe");
        assertDriverIsInstalled("geckodriver-mac-64bit");
        assertDriverIsInstalled("geckodriver-linux-64bit");
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
        assertDriverIsInstalled("chromedriver-mac-32bit");
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
