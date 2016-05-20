package com.github.webdriverextensions;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.webdriverextensions.Utils.quote;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

public class DriverDownloader {

    public static final int FILE_DOWNLOAD_READ_TIMEOUT = 30 * 60 * 1000; // 30 min
    public static final int FILE_DOWNLOAD_CONNECT_TIMEOUT = 30 * 1000; // 30 seconds
    public static final int FILE_DOWNLOAD_RETRY_ATTEMPTS = 3;
    private final InstallDriversMojo mojo;
    private final Proxy proxySettings;

    public DriverDownloader(InstallDriversMojo mojo) throws MojoExecutionException {
        this.mojo = mojo;
        this.proxySettings = ProxyUtils.getProxyFromSettings(mojo);
    }

    public Path downloadFile(Driver driver, File downloadDirectory) throws MojoExecutionException {

        String url = driver.getUrl();
        Path downloadFilePath = Paths.get(downloadDirectory.getPath(), driver.getFilenameFromUrl());

        if (downloadFilePath.toFile().exists() && !downloadCompletedFileExists(downloadDirectory)) {
            mojo.getLog().info("  Removing cached driver " + quote(downloadFilePath) + " since it may be corrupt");
            cleanupDriverCacheDirectory(downloadDirectory);
        }

        if (downloadFilePath.toFile().exists()) {
            mojo.getLog().info("  Using cached driver from " + quote(downloadFilePath));
        } else {
            mojo.getLog().info("  Downloading " + quote(url) + " to " + quote(downloadFilePath));
            HttpClientBuilder httpClientBuilder = prepareHttpClientBuilderWithTimeoutsAndProxySettings(proxySettings);
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(FILE_DOWNLOAD_RETRY_ATTEMPTS, true));
            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                try (CloseableHttpResponse fileDownloadResponse = httpClient.execute(new HttpGet(url))) {
                    HttpEntity remoteFileStream = fileDownloadResponse.getEntity();
                    copyInputStreamToFile(remoteFileStream.getContent(), downloadFilePath.toFile());
                    if (driverFileIsCorrupt(downloadFilePath)) {
                        printXmlFileContetIfPresentInDonwloadedFile(downloadFilePath);
                        cleanupDriverCacheDirectory(downloadDirectory);
                        throw new InstallDriversMojoExecutionException("Failed to download a non corrupt driver", mojo, driver);
                    }
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

    private void printXmlFileContetIfPresentInDonwloadedFile(Path downloadFilePath) {
        try {
            List<String> fileContent = Files.readAllLines(Paths.get("/Users/anders/Temp/cache/old/phantomjs-linux-64bit-1.9.8/phantomjs-1.9.8-linux-x86_64.tar.bz2"), StandardCharsets.UTF_8);
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

    private HttpClientBuilder prepareHttpClientBuilderWithTimeoutsAndProxySettings(Proxy proxySettings) throws MojoExecutionException {
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(FILE_DOWNLOAD_READ_TIMEOUT).build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(FILE_DOWNLOAD_CONNECT_TIMEOUT).build();
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(requestConfig)
                .disableContentCompression();
        HttpHost proxy = ProxyUtils.createProxyFromSettings(proxySettings);
        if (proxy != null) {
            httpClientBuilder.setProxy(proxy);
            CredentialsProvider proxyCredentials = ProxyUtils.createProxyCredentialsFromSettings(proxySettings);
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


    public void cleanupDriverCacheDirectory(File downloadDirectory) throws MojoExecutionException {
        try {
            FileUtils.deleteDirectory(downloadDirectory);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to delete driver cache directory:" + System.lineSeparator()
                    + Utils.directoryToString(downloadDirectory), e);
        }
    }

    private boolean downloadCompletedFileExists(File downloadDirectory) {
        Path downloadCompletedFile = Paths.get(downloadDirectory.getPath(), "download.completed");
        return downloadCompletedFile.toFile().exists();
    }

    private void createDownloadCompletedFile(File downloadDirectory) throws InstallDriversMojoExecutionException {
        Path downloadCompletedFile = Paths.get(downloadDirectory.getPath(), "download.completed");
        try {
            Files.createFile(downloadCompletedFile);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to create download.completed file at " + downloadCompletedFile, e);

        }
    }
}
