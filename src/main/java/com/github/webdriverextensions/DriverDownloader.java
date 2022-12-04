package com.github.webdriverextensions;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import static com.github.webdriverextensions.Utils.quote;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DriverDownloader implements Closeable {

    private final org.apache.maven.plugin.logging.Log log;
    private CloseableHttpClient httpClient;
    private int connectTimeout;
    private int responseTimeout;
    private int maxRetries;
    private int retryDelay;
    private Optional<Proxy> proxy = Optional.empty();

    void open() {
        httpClient = createHttpClient();
    }

    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    DriverDownloader withTimeouts(int connectTimeout, int responseTimeout) {
        this.connectTimeout = connectTimeout;
        this.responseTimeout = responseTimeout;
        return this;
    }

    DriverDownloader withProxy(final @Nonnull Optional<Proxy> proxy) {
        this.proxy = proxy;
        return this;
    }

    DriverDownloader withRetry(int maxRetries, int retryDelay) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        return this;
    }

    Path downloadFile(Driver driver, Path baseDownloadDirectory) throws MojoExecutionException {
        String url = driver.getUrl();
        Path downloadDirectory = baseDownloadDirectory.resolve(driver.getDriverDownloadDirectoryName());
        Path downloadFilePath = downloadDirectory.resolve(driver.getFilenameFromUrl());

        if (downloadFilePath.toFile().exists() && downloadCompletedFileExists(downloadDirectory)) {
            log.info("  Using cached driver from " + quote(downloadFilePath));
        } else {
            log.info("  Downloading " + quote(url) + " to " + quote(downloadFilePath));
            try {
                httpClient.execute(new HttpGet(url), fileDownloadResponse -> {
                    final int statusCode = fileDownloadResponse.getCode();
                    if (HttpStatus.SC_OK == statusCode) {
                        Files.createDirectories(downloadFilePath);
                        Files.copy(fileDownloadResponse.getEntity().getContent(), downloadFilePath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        throw new ClientProtocolException(String.valueOf(statusCode));
                    }
                    return null;
                });
            } catch (ClientProtocolException e) {
                throw new InstallDriversMojoExecutionException("Download failed with status code " + e.getLocalizedMessage(), driver, null);
            } catch (IOException e) {
                throw new InstallDriversMojoExecutionException("Failed to download driver from " + quote(url) + " to " + quote(downloadFilePath), driver, e);
            }
            createDownloadCompletedFile(downloadDirectory);
        }
        return downloadFilePath;
    }

    private CloseableHttpClient createHttpClient() {
        final ConnectionConfig connConfig = ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(connectTimeout)).build();
        final PoolingHttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create().setDefaultConnectionConfig(connConfig).build();
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(responseTimeout))
                .build()
        )
                .disableCookieManagement()
                .disableContentCompression()
                .setConnectionManager(connManager)
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(maxRetries, TimeValue.ofSeconds(retryDelay)));

        proxy.ifPresent(proxy -> {
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
                throw new InstallDriversMojoExecutionException("Failed to create download.completed file at " + quote(downloadCompletedFile), e);
            }
        }
    }
}
