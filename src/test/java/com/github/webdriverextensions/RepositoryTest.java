package com.github.webdriverextensions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class RepositoryTest extends LocalServerTestBase {

    @Test
    public void testConstructor() throws MojoExecutionException, MalformedURLException {
        URL repositoryFile = getCompleteUrlFor("/repository-3.0.json");
        Driver driver = Repository.load(repositoryFile, Optional.empty()).getDrivers("chromedriver", "linux", "32", "2.9").get(0);

        assertThat(driver.getName(), is("chromedriver"));
        assertThat(driver.getPlatform(), is("linux"));
        assertThat(driver.getBit(), is("32"));
        assertThat(driver.getComparableVersion(), is(new ComparableVersion("2.9")));
        assertThat(driver.getUrl(), is("http://chromedriver.storage.googleapis.com/2.9/chromedriver_linux32.zip"));
    }

    @Test
    public void testLoadWithInvalidUrl() {
        InstallDriversMojoExecutionException e = assertThrows(InstallDriversMojoExecutionException.class, () -> {
            // hostname missing
            Repository.load(new URL("ftp:///"), Optional.empty());
        });
        assertThat(e.getMessage(), startsWith("Failed to download repository from url"));
        assertThat(e.getCause(), instanceOf(IOException.class));
    }

    @Test
    public void testLoadWithFileNotFound() {
        InstallDriversMojoExecutionException e = assertThrows(InstallDriversMojoExecutionException.class, () -> {
            Repository.load(getCompleteUrlFor("/404"), Optional.empty());
        });
        assertThat(e.getMessage(), startsWith("Failed to download repository from url"));
        assertThat(e.getCause(), instanceOf(HttpResponseException.class));
        assertThat(e.getCause().getMessage(), is("status code: 404, reason phrase: Not Found"));
    }

    @Test
    public void testLoadWithInvalidJson() {
        InstallDriversMojoExecutionException e = assertThrows(InstallDriversMojoExecutionException.class, () -> {
            Repository.load(getCompleteUrlFor("/invalid.json"), Optional.empty());
        });
        assertThat(e.getMessage(), startsWith("Failed to parse repository json"));
        assertThat(e.getCause(), instanceOf(NullPointerException.class));
        assertThat(e.getCause().getMessage(), is("repository json is empty"));
    }

    @Before
    public void setUp() throws IOException {
        server.registerHandler("/repository-3.0.json", (request, response, context) -> {
            response.setCode(HttpStatus.SC_OK);
            response.setEntity(new InputStreamEntity(getClass().getResource("/repository-3.0.json").openStream(), ContentType.APPLICATION_JSON));
        });
        server.registerHandler("/404", (request, response, context) -> {
            response.setCode(HttpStatus.SC_NOT_FOUND);
        });
        server.registerHandler("/invalid.json", (request, response, context) -> {
            response.setCode(HttpStatus.SC_OK);
            response.setEntity(new StringEntity(""));
        });
        start();
    }

}
