package com.github.webdriverextensions;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.maven.plugin.MojoExecutionException;

import static com.github.webdriverextensions.Utils.quote;

class DriverDownloader implements Closeable {

    private final InstallDriversMojo mojo;
    private final CloseableHttpClient httpClient;

    public DriverDownloader(InstallDriversMojo mojo) throws MojoExecutionException {
        this.mojo = mojo;
        httpClient = createHttpClient();
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    public Path downloadFile(Driver driver, Path baseDownloadDirectory) throws MojoExecutionException {
        String url = driver.getUrl();
        Path downloadDirectory = baseDownloadDirectory.resolve(driver.getDriverDownloadDirectoryName());
        Path downloadFilePath = downloadDirectory.resolve(driver.getFilenameFromUrl());

        if (downloadFilePath.toFile().exists() && downloadCompletedFileExists(downloadDirectory)) {
            mojo.getLog().info("  Using cached driver from " + quote(downloadFilePath));
        } else {
            mojo.getLog().info("  Downloading " + quote(url) + " to " + quote(downloadFilePath));
            try (CloseableHttpResponse fileDownloadResponse = httpClient.execute(new HttpGet(url))) {
                HttpEntity remoteFileStream = fileDownloadResponse.getEntity();
                final int statusCode = fileDownloadResponse.getCode();
                if (HttpStatus.SC_OK == statusCode) {
                    Files.createDirectories(downloadFilePath);
                    Files.copy(remoteFileStream.getContent(), downloadFilePath, StandardCopyOption.REPLACE_EXISTING);
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

    private CloseableHttpClient createHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(mojo.downloadConnectTimeout))
                .setResponseTimeout(Timeout.ofSeconds(mojo.downloadResponseTimeout))
                .build()
        )
                .disableCookieManagement()
                .disableContentCompression()
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(mojo.downloadMaxRetries, TimeValue.ofSeconds(mojo.downloadRetryDelay)));

        ProxyUtils.getProxyFromSettings(mojo.settings, mojo.proxyId).ifPresent(proxy -> {
            ProxyUtils.createProxyFromSettings(proxy).ifPresent(httpClientBuilder::setProxy);
            ProxyUtils.createProxyCredentialsFromSettings(proxy).ifPresent(httpClientBuilder::setDefaultCredentialsProvider);
        });
        return httpClientBuilder.build();
    }

    private boolean downloadCompletedFileExists(Path downloadDirectory) {
        Path downloadCompletedFile = downloadDirectory.resolve("download.completed");
        return Files.exists(downloadCompletedFile);
    }

    private void createDownloadCompletedFile(Path downloadDirectory) throws InstallDriversMojoExecutionException {
        Path downloadCompletedFile = downloadDirectory.resolve("download.completed");
        if (!Files.exists(downloadCompletedFile)) {
            try {
                Files.createFile(downloadCompletedFile);
            } catch (IOException e) {
                throw new InstallDriversMojoExecutionException("Failed to create download.completed file at " + downloadCompletedFile, e);
            }
        }
    }
}
