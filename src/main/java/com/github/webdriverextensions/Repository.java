package com.github.webdriverextensions;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import static com.github.webdriverextensions.Utils.detectArch;
import static com.github.webdriverextensions.Utils.detectPlatform;
import static com.github.webdriverextensions.Utils.isWindows10;
import static com.github.webdriverextensions.Utils.quote;
import static org.codehaus.plexus.util.StringUtils.isBlank;
import static org.codehaus.plexus.util.StringUtils.isNotBlank;
import static org.codehaus.plexus.util.StringUtils.trim;

class Repository {

    @Expose
    private List<Driver> drivers;

    static Repository load(URL repositoryUrl, Optional<Proxy> proxySettings) throws MojoExecutionException {
        String repositoryAsString;
        try {
            repositoryAsString = downloadAsString(repositoryUrl.toURI(), proxySettings);
        } catch (IOException | URISyntaxException e) {
            throw new InstallDriversMojoExecutionException("Failed to download repository from url " + quote(
                    repositoryUrl), e);
        }
        if (isBlank(trim(repositoryAsString))) {
            throw new InstallDriversMojoExecutionException("repository file is empty");
        }

        final Repository repository = new Repository();
        try {
            repository.drivers = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create()
                    .fromJson(repositoryAsString, Repository.class)
                    .drivers;
        } catch (JsonSyntaxException e) {
            throw new InstallDriversMojoExecutionException("Failed to parse repository json " + quote(repositoryUrl), e);
        }

        repository.drivers.sort(driversComparator());

        return repository;
    }

    private static Comparator<Driver> driversComparator() {
        Comparator<Driver> byId = new DriverComparator.ById();
        // sort by version descending (newest first)
        Comparator<Driver> byVersion = new DriverComparator.ByVersion().reversed();
        // sort by arch, unknown last
        Comparator<Driver> byArch = new DriverComparator.ByArch();

        return byId.thenComparing(byVersion).thenComparing(byArch);
    }

    private static String downloadAsString(URI url, Optional<Proxy> proxySettings) throws IOException {
        // kept vor backward compatibility
        if ("file".equalsIgnoreCase(url.getScheme())) {
            return Files.lines(Paths.get(url)).collect(Collectors.joining());
        }
        HttpClientBuilder httpClientBuilder = HttpClients.custom().disableCookieManagement();
        proxySettings.ifPresent(proxy -> {
            ProxyUtils.createProxyFromSettings(proxy).ifPresent(httpClientBuilder::setProxy);
            ProxyUtils.createProxyCredentialsFromSettings(proxy).ifPresent(httpClientBuilder::setDefaultCredentialsProvider);
        });
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            return httpClient.execute(new HttpGet(url), new BasicHttpClientResponseHandler());
        }
    }

    List<Driver> getDrivers(@Nullable String name, @Nullable String platform, @Nullable String bit, @Nullable Architecture arch, @Nullable String version) {
        return filterDrivers(drivers, name, platform, bit, arch, version);
    }

    private List<Driver> filterDrivers(@Nonnull List<Driver> driversToFilter, @Nullable String name, @Nullable String platform, @Nullable String bit, @Nullable Architecture arch, @Nullable String version) {
        return driversToFilter.stream()
                .filter(driver -> name != null ? name.equalsIgnoreCase(driver.getName()) : true)
                .filter(driver -> platform != null ? platform.equalsIgnoreCase(driver.getPlatform()) : true)
                .filter(driver -> bit != null ? bit.equalsIgnoreCase(driver.getBit()) : true)
                .filter(driver -> arch != null && isNotBlank(driver.getArch()) ? arch == driver.getArchitecture() : true)
                .filter(driver -> version != null ? new ComparableVersion(version).equals(driver.getComparableVersion()) : true)
                .collect(Collectors.toList());
    }

    Driver enrichDriver(Driver driver) throws MojoExecutionException {
        if (isBlank(driver.getName())) {
            throw new InstallDriversMojoExecutionException("Driver name must be set in configuration", driver, null);
        }
        if (isNotBlank(driver.getUrl())) {
            return driver;
        }
        if (isNotBlank(driver.getPlatform()) || isNotBlank(driver.getBit()) || isNotBlank(driver.getVersion())) {
            // Explicit driver config make sure it exists in repo
            if (getDrivers(driver.getName(), driver.getPlatform(), driver.getBit(), null, driver.getVersion()).isEmpty()) {
                throw new InstallDriversMojoExecutionException("Could not find driver", driver, null);
            }
        }

        if (isBlank(driver.getPlatform())) {
            driver.setPlatform(detectPlatform());
        }
        if (isBlank(driver.getBit())) {
            driver.setBit(detectBits(driver.getName()));
        }
        if (isBlank(driver.getVersion())) {
            driver.setVersion(getLatestDriverVersion(driver.getId()));
        }
        if (isBlank(driver.getArch())) {
            driver.setArch(detectArch().toString());
        }

        List<Driver> drivers = getDrivers(driver.getName(),
                                          driver.getPlatform(),
                                          driver.getBit(),
                                          driver.getArchitecture(),
                                          driver.getVersion());
        if (drivers.isEmpty()) {
            if ("64".equals(driver.getBit())) {
                // toogle bits and try the other bit to get a driver configuration
                drivers = getDrivers(driver.getName(),
                                     driver.getPlatform(),
                                     "32",
                                     driver.getArchitecture(),
                                     driver.getVersion());

                if (drivers.isEmpty()) {
                    // Could not find any driver for the current platform/bit/version in repo
                    return null;
                }
                return transferCustomFileName(driver,filterLatestDriver(drivers));
            }
            return null;
        }

        return transferCustomFileName(driver, drivers.get(0));
    }

    private Driver transferCustomFileName(Driver driver, Driver foundDriver) {

        if (isNotBlank(driver.getCustomFileName())) {
            foundDriver.setCustomFileName(driver.getCustomFileName());
        }

        return foundDriver;
    }

    List<Driver> getLatestDrivers() {
        String platform = detectPlatform();
        Architecture arch = detectArch();
        return drivers.stream()
                .map(Driver::getName)
                .distinct()
                .map(driverName -> {
                    List<Driver> driversWithDriverNameAndPlatform = getDrivers(driverName, platform, null, arch, null);
                    String bit = detectBits(driverName);
                    Driver latestDriver = getDriverByBit(bit, driversWithDriverNameAndPlatform);
                    if (latestDriver == null && bit.equals("64")) {
                        // try to find the 32-bit driver if no 64-bit driver was found
                        latestDriver = getDriverByBit("32", driversWithDriverNameAndPlatform);
                    }
                    return latestDriver;
                })
                .filter(Objects::nonNull)
                .sorted(driversComparator())
                .collect(Collectors.toList());
    }

    private Driver getDriverByBit(String bit, List<Driver> driversWithDriverNameAndPlatform) {
        List<Driver> driversWithDriverNameAndPlatformAndBit = filterDrivers(driversWithDriverNameAndPlatform, null, null, bit, null, null);
        return filterLatestDriver(driversWithDriverNameAndPlatformAndBit);
    }

    private static String detectBits(String driverName) {
        // Default installed internetexplorer bit version on < Windows 10 versions is 32 bit
        if (driverName.equals("internetexplorerdriver") && !isWindows10()) {
            return "32";
        } else {
        // Detect bit version from os
            return Utils.detectBits();
        }
    }

    private String getLatestDriverVersion(String driverId) {
        return drivers.stream()
                .filter(driver -> driverId.equalsIgnoreCase(driver.getId()))
                .sorted(driversComparator())
                .findFirst()
                .map(Driver::getVersion).orElse(null);
    }

    private Driver filterLatestDriver(List<Driver> drivers) {
        return drivers.stream().sorted(driversComparator()).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(this);
    }
}
