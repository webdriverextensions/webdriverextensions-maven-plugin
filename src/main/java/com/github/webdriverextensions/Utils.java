package com.github.webdriverextensions;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import ch.lambdaj.function.compare.ArgumentComparator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.ComparatorUtils;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import org.apache.commons.io.IOUtils;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.openqa.selenium.Platform;

public class Utils {

    public static final int FILE_DOWNLOAD_READ_TIMEOUT = 30 * 60 * 1000; // 30 min
    public static final int FILE_DOWNLOAD_CONNECT_TIMEOUT = 30 * 1000; // 30 seconds
    public static final int FILE_DOWNLOAD_RETRY_ATTEMPTS = 3;

    public static boolean directoryContainsSingleDirectory(String directory) {
        File[] files = new File(directory).listFiles();
        return files != null && files.length == 1 && files[0].isDirectory();
    }

    public static boolean directoryContainsSingleFile(String directory) throws MojoExecutionException {
        File[] files = new File(directory).listFiles();
        return files != null && files.length == 1 && files[0].isFile();
    }

    public static void moveDirectoryInDirectory(String from, String to) throws MojoExecutionException {
        assert directoryContainsSingleDirectory(from);
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(new File(from), null, null, true);
            FileUtils.rename(new File(subDirectories.get(1)), new File(to));
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving direcotry in directory " + quote(from) + " to " + quote(to), ex);
        }
    }

    public static void moveFileInDirectory(String from, String to) throws MojoExecutionException {
        assert directoryContainsSingleFile(from);
        try {
            List<String> files = FileUtils.getFileNames(new File(from), null, null, true);
            FileUtils.rename(new File(files.get(0)), new File(to));
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving file in directory " + quote(from) + " to " + quote(to), ex);
        }
    }

    public static void moveAllFilesInDirectory(String from, String to) throws MojoExecutionException {
        try {
            List<String> subDirectories = FileUtils.getDirectoryNames(new File(from), null, null, true);
            FileUtils.rename(new File(subDirectories.get(0)), new File(to));
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when moving direcotry " + quote(from) + " to " + quote(to), ex);
        }
    }

    public static String calculateChecksum(String fileOrDirectory) throws MojoExecutionException {
        if (new File(fileOrDirectory).isDirectory()) {
            return calculateChecksumForDirectory(fileOrDirectory);
        } else if (new File(fileOrDirectory).isFile()) {
            return calculateChecksumForFile(fileOrDirectory);
        }
        throw new MojoExecutionException("File or directory does not exist " + quote(fileOrDirectory));
    }

    private static String calculateChecksumForFile(String file) throws MojoExecutionException {
        try (FileInputStream fileInputStream = new FileInputStream(new File(file))){
            return DigestUtils.md5Hex(fileInputStream);
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when calculating checksum for file " + quote(file), ex);
        }
    }

    private static String calculateChecksumForDirectory(String directory) throws MojoExecutionException {
        try {
            // Collect all files in directory as streams
            List<FileInputStream> fileStreams = new ArrayList<>();
            for (Object file : FileUtils.getFiles(new File(directory), null, null)) {
                fileStreams.add(new FileInputStream((File) file));
            }
            try (SequenceInputStream sequenceInputStream = new SequenceInputStream(Collections.enumeration(fileStreams))){
                return DigestUtils.md5Hex(sequenceInputStream);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Error when calculating checksum for directory " + quote(directory), ex);
        }
    }

    public static void downloadFile(String url, Path downloadLocation, Log log, Proxy proxySettings) throws MojoExecutionException {
        log.info("  Downloading " + url + " -> " + downloadLocation);
        File fileToDownload = downloadLocation.toFile();
        if (fileToDownload.exists()) {
            log.info("file " + downloadLocation + " already downloaded");
        } else {
            HttpClientBuilder httpClientBuilder = prepareHttpClientBuilderWithTimeoutsAndProxySettings(proxySettings);
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(FILE_DOWNLOAD_RETRY_ATTEMPTS, true));
            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                try (CloseableHttpResponse fileDownloadResponse = httpClient.execute(new HttpGet(url))) {
                    HttpEntity remoteFileStream = fileDownloadResponse.getEntity();
                    copyInputStreamToFile(remoteFileStream.getContent(), fileToDownload);
                }
            } catch (IOException ex) {
                log.info("Problem downloading file from " + url + " cause of " + ex.getCause());
                throw new MojoExecutionException("Failed to download file", ex);
            }
        }
    }

    private static HttpClientBuilder prepareHttpClientBuilderWithTimeoutsAndProxySettings(Proxy proxySettings) throws MojoExecutionException {
        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(FILE_DOWNLOAD_READ_TIMEOUT).build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(FILE_DOWNLOAD_CONNECT_TIMEOUT).build();
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(requestConfig)
                .disableContentCompression();
        HttpHost proxy = createProxyFromSettings(proxySettings);
        if (proxy != null) {
            httpClientBuilder.setProxy(proxy);
            CredentialsProvider proxyCredentials = createProxyCredentialsFromSettings(proxySettings);
            if (proxyCredentials != null) {
                httpClientBuilder.setDefaultCredentialsProvider(proxyCredentials);
            }
        }
        return httpClientBuilder;
    }

    public static List<Driver> sortDrivers(List<Driver> drivers) {
        Comparator byId = new ArgumentComparator(on(Driver.class).getId());
        Comparator byVersion = new ArgumentComparator(on(Driver.class).getVersion());
        Comparator orderByIdAndVersion = ComparatorUtils.chainedComparator(byId, byVersion);

        return sort(drivers, on(Driver.class), orderByIdAndVersion);
    }

    public static final String quote(String text) {
        return "\"" + text + "\"";
    }

    public static final String quote(File file) {
        return quote(file.getAbsolutePath());
    }

    public static final String quote(URL url) {
        return quote(url.toString());
    }

    public static boolean isWindows() {
        return Platform.WINDOWS.is(Platform.getCurrent());
    }

    public static boolean isMac() {
        return Platform.MAC.is(Platform.getCurrent());
    }

    public static boolean isLinux() {
        return Platform.LINUX.is(Platform.getCurrent());
    }

    public static boolean is64Bit() {
        return com.sun.jna.Platform.is64Bit();
    }

    public static void makeExecutable(String path) {
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (file.exists() && !file.canExecute()) {
            file.setExecutable(true);
        }
    }

    public static Proxy getProxyFromSettings(Settings settings, String proxyId) throws MojoExecutionException {
        if (settings == null) {
            return null;
        }

        if (proxyId != null) {
            for (Proxy proxy : settings.getProxies()) {
                if (proxyId.equals(proxy.getId())) {
                    return proxy;
                }
            }
            throw new MojoExecutionException("Configured proxy with id=" + proxyId + " not found in settings.xml");
        }

        // Get active http/https proxy
        for (Proxy proxy : settings.getProxies()) {
            if (proxy.isActive() && ("http".equalsIgnoreCase(proxy.getProtocol()) || "https".equalsIgnoreCase(proxy.getProtocol()))) {
                return proxy;
            }
        }

        return null;
    }

    public static HttpHost createProxyFromSettings(Proxy proxySettings) throws MojoExecutionException {
        if (proxySettings == null) {
            return null;
        }
        return new HttpHost(proxySettings.getHost(), proxySettings.getPort());
    }

    public static String downloadAsString(URL url, Proxy proxySettings) throws IOException {
        URLConnection connection;
        if (proxySettings != null) {
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP,
                    new InetSocketAddress(proxySettings.getHost(), proxySettings.getPort()));
            if (proxySettings.getUsername() != null) {
                setProxyAuthenticator(proxySettings);
            }
            connection = url.openConnection(proxy);
        } else {
            connection = url.openConnection();
        }

        final InputStream inputStream = connection.getInputStream();
        try {
            return IOUtils.toString(inputStream, UTF_8);
        } finally {
            inputStream.close();
        }
    }

    private static CredentialsProvider createProxyCredentialsFromSettings(Proxy proxySettings) throws MojoExecutionException {
        if (proxySettings.getUsername() == null) {
            return null;
        }
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(proxySettings.getUsername(), proxySettings.getPassword()));

        return credentialsProvider;
    }

    private static void setProxyAuthenticator(final Proxy proxy) {
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return (new PasswordAuthentication(proxy.getUsername(),
                        proxy.getPassword().toCharArray()));
            }
        };
        Authenticator.setDefault(authenticator);
    }
}
