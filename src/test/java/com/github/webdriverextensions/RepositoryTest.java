package com.github.webdriverextensions;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.maven.plugin.MojoExecutionException;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import org.junit.Test;

public class RepositoryTest {

    @Test
    //@Ignore // Ignore this test if you use a proxy since it will fail
    public void testConstructor() throws MojoExecutionException, InterruptedException, MalformedURLException {
        URL repositoryFile = new URL("file://" + getClass().getResource("/repository.json").getPath());
        Driver driver = Repository.load(repositoryFile, null).getDrivers("chromedriver", "linux", "32", "2.9").get(0);

        assertThat(driver.getName(), is("chromedriver"));
        assertThat(driver.getPlatform(), is("linux"));
        assertThat(driver.getBit(), is("32"));
        assertThat(driver.getComparableVersion(), is(new ComparableVersion("2.9")));
        assertThat(driver.getUrl(), is("http://chromedriver.storage.googleapis.com/2.9/chromedriver_linux32.zip"));
        assertThat(driver.getChecksum(), is("1047354165f02aec8ac05f0b0316fe57"));

    }

}
