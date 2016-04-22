package com.github.webdriverextensions;

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
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public Path downloadFile(Driver driver, File downloadPath) throws MojoExecutionException {

        String url = driver.getUrl();
        Path downloadLocation = Paths.get(downloadPath.getPath(), driver.getFilenameFromUrl());

        File fileToDownload = downloadLocation.toFile();
        if (fileToDownload.exists()) {
            mojo.getLog().info("  Using cached driver from " + downloadLocation);
        } else {
            mojo.getLog().info("  Downloading " + url + " to " + downloadLocation);
            HttpClientBuilder httpClientBuilder = prepareHttpClientBuilderWithTimeoutsAndProxySettings(proxySettings);
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(FILE_DOWNLOAD_RETRY_ATTEMPTS, true));
            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                try (CloseableHttpResponse fileDownloadResponse = httpClient.execute(new HttpGet(url))) {
                    HttpEntity remoteFileStream = fileDownloadResponse.getEntity();
                    copyInputStreamToFile(remoteFileStream.getContent(), fileToDownload);
                }
            } catch (IOException e) {
                mojo.getLog().info("  Failed to download driver cause of " + e.getCause() + Utils.debugInfo(mojo, driver), e);
                throw new InstallDriversMojoExecutionException("Failed to download driver cause of " + e.getCause(), e, mojo, driver);
            }
        }
        return downloadLocation;
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

}
