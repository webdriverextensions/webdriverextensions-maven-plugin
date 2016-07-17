package com.github.webdriverextensions;

public class InstallChromedriverMojoTest extends AbstractInstallDriversMojoTest {


    public void test_chromedriver_bug() throws Exception {
        // Given
        InstallDriversMojo mojo = getMojo("src/test/resources/chromedriver_pom.xml");
        mojo.keepDownloadedWebdrivers = false;


        // When
        mojo.execute();

        // Then
        assertDriverIsInstalled("chromedriver-windows-32bit");
        assertNumberOfInstalledDriverIs(1);
    }

}
