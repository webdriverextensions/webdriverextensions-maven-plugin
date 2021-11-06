package com.github.webdriverextensions;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.webdriverextensions.Utils.quote;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

public class DriverDownloader implements Closeable {

    public static final int FILE_DOWNLOAD_READ_TIMEOUT = 30 * 60 * 1000; // 30 min
    public static final int FILE_DOWNLOAD_CONNECT_TIMEOUT = 30 * 1000; // 30 seconds
    public static final int FILE_DOWNLOAD_RETRY_ATTEMPTS = 3;
    private final InstallDriversMojo mojo;
    private final CloseableHttpClient httpClient;

    public DriverDownloader(InstallDriversMojo mojo) throws MojoExecutionException {
        this.mojo = mojo;
        httpClient = prepareHttpClientBuilderWithTimeoutsAndProxySettings(ProxyUtils.getProxyFromSettings(mojo)).build();
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    public Path downloadFile(Driver driver, Path downloadDirectory) throws MojoExecutionException {

        String url = driver.getUrl();
        Path downloadFilePath = downloadDirectory.resolve(driver.getFilenameFromUrl());

        if (downloadFilePath.toFile().exists() && !downloadCompletedFileExists(downloadDirectory)) {
            mojo.getLog().info("  Removing downloaded driver " + quote(downloadFilePath) + " since it may be corrupt");
            cleanupDriverDownloadDirectory(downloadDirectory);
        } else if (!mojo.keepDownloadedWebdrivers) {
            cleanupDriverDownloadDirectory(downloadDirectory);
        }

        if (downloadFilePath.toFile().exists()) {
            mojo.getLog().info("  Using cached driver from " + quote(downloadFilePath));
        } else {
            mojo.getLog().info("  Downloading " + quote(url) + " to " + quote(downloadFilePath));
            try (CloseableHttpResponse fileDownloadResponse = httpClient.execute(new HttpGet(url))) {
                HttpEntity remoteFileStream = fileDownloadResponse.getEntity();
                final int statusCode = fileDownloadResponse.getCode();
                if (HttpStatus.SC_OK == statusCode) {
                    copyInputStreamToFile(remoteFileStream.getContent(), downloadFilePath.toFile());
                    if (driverFileIsCorrupt(downloadFilePath)) {
                        printXmlFileContentIfPresentInDownloadedFile(downloadFilePath);
                        cleanupDriverDownloadDirectory(downloadDirectory);
                        throw new InstallDriversMojoExecutionException("Failed to download a non corrupt driver", mojo, driver);
                    }
                } else {
                    throw new InstallDriversMojoExecutionException("Download failed with status code " + statusCode, mojo, driver);
                }
            } catch (InstallDriversMojoExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new InstallDriversMojoExecutionException("Failed to download driver from " + quote(url) + " to " + quote(downloadFilePath) + " cause of " + e.getCause(), e, mojo, driver);
            }
            createDownloadCompletedFile(downloadDirectory);
        }
        return downloadFilePath;
    }

    private void printXmlFileContentIfPresentInDownloadedFile(Path downloadFilePath) {
        try {
            List<String> fileContent = Files.readAllLines(downloadFilePath, StandardCharsets.UTF_8);
            if (fileContent.get(0).startsWith("<?xml")) {
                mojo.getLog().info("  Downloaded driver file contains the following error message");
                for (String line : fileContent) {
                    mojo.getLog().info("  " + line);
                }
            }
        } catch (Exception e) {
            // no file  or file content to read
        }
    }

    private HttpClientBuilder prepareHttpClientBuilderWithTimeoutsAndProxySettings(Proxy proxySettings) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(FILE_DOWNLOAD_CONNECT_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(FILE_DOWNLOAD_READ_TIMEOUT))
                .build()
        )
                .disableCookieManagement()
                .disableContentCompression()
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(FILE_DOWNLOAD_RETRY_ATTEMPTS, TimeValue.ofSeconds(1)));
        HttpHost proxy = ProxyUtils.createProxyFromSettings(proxySettings);
        if (proxy != null) {
            httpClientBuilder.setProxy(proxy);
            CredentialsProvider proxyCredentials = ProxyUtils.createProxyCredentialsFromSettings(proxySettings, proxy);
            if (proxyCredentials != null) {
                httpClientBuilder.setDefaultCredentialsProvider(proxyCredentials);
            }
        }
        return httpClientBuilder;
    }

    private boolean driverFileIsCorrupt(Path downloadFilePath) {
        if (Utils.hasExtension(downloadFilePath, "zip")) {
            return !Utils.validateZipFile(downloadFilePath);
        } else if (Utils.hasExtension(downloadFilePath, "bz2")) {
            if (!Utils.validateBz2File(downloadFilePath)) {
                return true;
            } else {
                return !Utils.validateFileIsLargerThanBytes(downloadFilePath, 1000);
            }
        } else {
            return false;
        }
    }


    public void cleanupDriverDownloadDirectory(Path downloadDirectory) throws MojoExecutionException {
        try {
            FileUtils.deleteDirectory(downloadDirectory.toFile());
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to delete driver cache directory:" + System.lineSeparator()
                    + Utils.directoryToString(downloadDirectory), e);
        }
    }

    private boolean downloadCompletedFileExists(Path downloadDirectory) {
        Path downloadCompletedFile = downloadDirectory.resolve("download.completed");
        return downloadCompletedFile.toFile().exists();
    }

    private void createDownloadCompletedFile(Path downloadDirectory) throws InstallDriversMojoExecutionException {
        Path downloadCompletedFile = downloadDirectory.resolve("download.completed");
        try {
            Files.createFile(downloadCompletedFile);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to create download.completed file at " + downloadCompletedFile, e);

        }
    }
}
