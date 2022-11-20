package com.github.webdriverextensions;

public class InstallDriversOnlyBrowserMojoTest
        extends AbstractInstallDriversMojoTest {
    private static final String CHROME_DRIVER_LATEST = "100.0.4896.20";
    private InstallDriversMojo mojoUnderTest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mojoUnderTest = getMojo("src/test/resources/only_browser_pom.xml");

    }

    public void test_on_windows_64() throws Exception {
        // Given
        fakePlatformToBeWindows();
        fakeBitToBe64();

        // When
        mojoUnderTest.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe", CHROME_DRIVER_LATEST);
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_on_windows_32() throws Exception {
        // Given
        fakePlatformToBeWindows();
        fakeBitToBe32();

        // When
        mojoUnderTest.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe", CHROME_DRIVER_LATEST);
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_on_windows10_64() throws Exception {
        // Given
        fakePlatformToBeWindows10();
        fakeBitToBe64();

        // When
        mojoUnderTest.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit.exe",
                CHROME_DRIVER_LATEST);
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_on_linux_64() throws Exception {
        // Given
        fakePlatformToBeLinux();
        fakeBitToBe64();

        // When
        mojoUnderTest.execute();

        // Then
        assertDriverIsInstalled("chromedriver-linux-64bit",
                CHROME_DRIVER_LATEST);
        assertNumberOfInstalledDriverIs(1);
    }

    public void test_on_linux_32() throws Exception {
        // Given
        fakePlatformToBeLinux();
        fakeBitToBe32();

        // When
        mojoUnderTest.execute();

        // Then
        assertDriverIsInstalled("chromedriver-linux-32bit", "2.33.0");
        assertNumberOfInstalledDriverIs(1);
    }
}
