package com.github.webdriverextensions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalServerTestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertThrows;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class RepositoryTest extends LocalServerTestBase {
    private HttpHost host;

    @Test
    public void testConstructor() throws MojoExecutionException, MalformedURLException {
        URL repositoryFile = new URL(String.format("http://%s:%d/repository-3.0.json", host.getHostName(), host.getPort()));
        Driver driver = Repository.load(repositoryFile, null).getDrivers("chromedriver", "linux", "32", "2.9").get(0);

        assertThat(driver.getName(), is("chromedriver"));
        assertThat(driver.getPlatform(), is("linux"));
        assertThat(driver.getBit(), is("32"));
        assertThat(driver.getComparableVersion(), is(new ComparableVersion("2.9")));
        assertThat(driver.getUrl(), is("http://chromedriver.storage.googleapis.com/2.9/chromedriver_linux32.zip"));
    }

    @Test
    public void testLoadWithInvalidUrl() {
        InstallDriversMojoExecutionException e = assertThrows(InstallDriversMojoExecutionException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                // hostname missing
                Repository.load(new URL("ftp:///"), null);
            }
        });
        assertThat(e.getMessage(), startsWith("Failed to download repository from url"));
        assertThat(e.getCause(), instanceOf(IOException.class));
    }

    @Test
    public void testLoadWithFileNotFound() {
        InstallDriversMojoExecutionException e = assertThrows(InstallDriversMojoExecutionException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                Repository.load(new URL(String.format("http://%s:%d/404", host.getHostName(), host.getPort())), null);
            }
        });
        assertThat(e.getMessage(), startsWith("Failed to download repository from url"));
        assertThat(e.getCause(), instanceOf(HttpResponseException.class));
        assertThat(e.getCause().getMessage(), is("status code: 404, reason phrase: Not Found"));
    }

    @Test
    public void testLoadWithInvalidJson() {
        InstallDriversMojoExecutionException e = assertThrows(InstallDriversMojoExecutionException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                Repository.load(new URL(String.format("http://%s:%d/invalid.json", host.getHostName(), host.getPort())), null);
            }
        });
        assertThat(e.getMessage(), startsWith("Failed to parse repository json"));
        assertThat(e.getCause(), instanceOf(NullPointerException.class));
        assertThat(e.getCause().getMessage(), is("repository json is empty"));
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        serverBootstrap.registerHandler("/repository-3.0.json", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new InputStreamEntity(getClass().getResource("/repository-3.0.json").openStream(), ContentType.APPLICATION_JSON));
            }
        });
        serverBootstrap.registerHandler("/404", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        });
        serverBootstrap.registerHandler("/invalid.json", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity(""));
            }
        });
        host = start();
    }
    
}
