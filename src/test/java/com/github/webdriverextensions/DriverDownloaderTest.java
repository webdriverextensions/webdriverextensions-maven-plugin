/*
 * Copyright 2021 WebDriver Extensions.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.webdriverextensions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.PathEntity;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.testing.classic.ClassicTestServer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@RunWith(MockitoJUnitRunner.class)
public class DriverDownloaderTest extends LocalServerTestBase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Mock
    private Log log;

    private int driverDownloadServerInvocations;
    private ClassicTestServer proxyServer;
    private Path downloadDirectory;

    @Test
    public void downloadFileWithMissingCompleteFile() throws Exception {
        Driver driver = new Driver();
        driver.setUrl(getCompleteUrlFor("/foo.zip").toString());

        try (final DriverDownloader uut = new DriverDownloader(log)) {
            uut.open();
            Path downloadFilePath = uut.downloadFile(driver, downloadDirectory);
            assertThat(downloadFilePath).exists();
            assertThat(driverDownloadServerInvocations).isOne();
        }
    }

    @Test
    public void downloadFileWithCompleteFileAlreadyPresent() throws Exception {
        Driver driver = new Driver();
        driver.setUrl(getCompleteUrlFor("/foo.zip").toString());
        Files.createDirectory(downloadDirectory.resolve(driver.getDriverDownloadDirectoryName()));
        Files.createFile(downloadDirectory.resolve(driver.getDriverDownloadDirectoryName()).resolve("download.completed"));

        try (final DriverDownloader uut = new DriverDownloader(log)) {
            uut.open();
            Path downloadFilePath = uut.downloadFile(driver, downloadDirectory);
            assertThat(downloadFilePath).exists();
            assertThat(driverDownloadServerInvocations).isOne();
        }
    }

    @Test
    public void downloadFileWithValidCachedShouldNotDownloadAnything() throws Exception {
        Driver driver = new Driver();
        driver.setUrl(getCompleteUrlFor("/foo.zip").toString());
        Files.createDirectory(downloadDirectory.resolve(driver.getDriverDownloadDirectoryName()));
        Files.createFile(downloadDirectory.resolve(driver.getDriverDownloadDirectoryName()).resolve("download.completed"));
        Files.createFile(downloadDirectory.resolve(driver.getDriverDownloadDirectoryName()).resolve("foo.zip"));

        try (final DriverDownloader uut = new DriverDownloader(log)) {
            uut.open();
            uut.downloadFile(driver, downloadDirectory);
            assertThat(driverDownloadServerInvocations).isZero();
        }
    }

    @Test
    public void testRetry() throws Exception {
        Driver driver = new Driver();
        driver.setUrl(getCompleteUrlFor("/429").toString());
        int downloadMaxRetries = 2;

        try (final DriverDownloader uut = new DriverDownloader(log).withTimeouts(1, 1).withRetry(downloadMaxRetries, 1)) {
            uut.open();
            assertThatCode(() -> uut.downloadFile(driver, downloadDirectory))
                    .isInstanceOf(InstallDriversMojoExecutionException.class).hasMessageStartingWith("Download failed with status code %d", HttpStatus.SC_TOO_MANY_REQUESTS);
            assertThat(downloadDirectory).isEmptyDirectory();
            assertThat(driverDownloadServerInvocations).isEqualTo(1 + downloadMaxRetries);
        }
    }

    @Test
    public void test404() throws Exception {
        Driver driver = new Driver();
        driver.setUrl(getCompleteUrlFor("/404").toString());

        try (final DriverDownloader uut = new DriverDownloader(log)) {
            uut.open();
            assertThatCode(() -> uut.downloadFile(driver, downloadDirectory))
                    .isInstanceOf(InstallDriversMojoExecutionException.class).hasMessageStartingWith("Download failed with status code %d", HttpStatus.SC_NOT_FOUND);
            assertThat(downloadDirectory).isEmptyDirectory();
            assertThat(driverDownloadServerInvocations).isOne();
        }
    }

    @Test
    public void testWithProxy() throws Exception {
        Driver driver = new Driver();
        driver.setUrl(getCompleteUrlFor("/proxy").toString());
        String proxyId = "test-proxy";
        Proxy proxy = new Proxy();
        proxy.setUsername("user");
        proxy.setPassword("pass");
        proxy.setId(proxyId);
        proxy.setHost("localhost");
        proxy.setProtocol("http");
        Settings settings = new Settings();
        settings.setProxies(Collections.singletonList(proxy));

        // create 2nd proxy server. verify that 1st server is not called. verify proxy was called twice: 1st without auth, 2nd with auth.
        proxyServer = new ClassicTestServer();
        final AtomicInteger proxyInvocations = new AtomicInteger();
        proxyServer.register("/proxy", (request, response, context) -> {
            proxyInvocations.incrementAndGet();
            if (!request.containsHeader(HttpHeaders.PROXY_AUTHORIZATION) || !"Basic dXNlcjpwYXNz".equals(request.getHeader(HttpHeaders.PROXY_AUTHORIZATION).getValue())) {
                // httpclient does not perform pre-emptive authentication by default.
                // header and status code are required to trigger a 2nd request but this time including the proxy credentials.
                response.setCode(HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED);
                response.addHeader(HttpHeaders.PROXY_AUTHENTICATE, "Basic");
            } else {
                // valid proxy credentials supplied, send a regular response
                response.setCode(HttpStatus.SC_NOT_FOUND);
            }
        });
        proxyServer.start();
        proxy.setPort(proxyServer.getPort());

        try (final DriverDownloader uut = new DriverDownloader(log).withProxy(ProxyUtils.getProxyFromSettings(settings, proxyId))) {
            uut.open();
            assertThatCode(() -> uut.downloadFile(driver, downloadDirectory)).isInstanceOf(InstallDriversMojoExecutionException.class);
            assertThat(driverDownloadServerInvocations).isZero();
            assertThat(downloadDirectory).isEmptyDirectory();
            assertThat(proxyInvocations).hasValue(2);
        }
    }

    @Before
    public void setUp() throws Exception {
        driverDownloadServerInvocations = 0;
        downloadDirectory = tempFolder.newFolder("download").toPath();

        server.register("/foo.zip", (request, response, context) -> {
            response.setCode(HttpStatus.SC_OK);
            response.setEntity(new PathEntity(Paths.get("src/test/resources/fake-drivers/chromedriver/chromedriver_win32.zip"), ContentType.create("application/zip")));
            driverDownloadServerInvocations++;
        });
        server.register("/429", (request, response, context) -> {
            response.setCode(HttpStatus.SC_TOO_MANY_REQUESTS);
            driverDownloadServerInvocations++;
        });
        server.register("/404", (request, response, context) -> {
            response.setCode(HttpStatus.SC_NOT_FOUND);
            driverDownloadServerInvocations++;
        });
        start();
    }

    @After
    public void tearDown() {
        if (proxyServer != null) {
            proxyServer.shutdown(CloseMode.IMMEDIATE);
        }
    }
}
