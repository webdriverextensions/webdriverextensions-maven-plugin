package com.github.webdriverextensions;

import static ch.lambdaj.Lambda.collect;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.Lambda.selectDistinct;
import static ch.lambdaj.Lambda.selectMax;
import static ch.lambdaj.Lambda.sort;
import ch.lambdaj.function.compare.ArgumentComparator;
import static com.github.webdriverextensions.Utils.is64Bit;
import static com.github.webdriverextensions.Utils.isLinux;
import static com.github.webdriverextensions.Utils.isMac;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.io.IOUtils;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

public class Repository {

    private List<Driver> drivers;

    public static Repository load(URL repositoryUrl, Proxy proxySettings) throws MojoExecutionException {
        String repositoryAsString;
        try {
            repositoryAsString = downloadAsString(repositoryUrl, proxySettings);
        } catch (IOException ex) {
            throw new MojoExecutionException("ERROR: Could not download repository from url " + Utils.quote(repositoryUrl), ex);
        }

        Repository repository;
        try {
            repository = new Gson().fromJson(repositoryAsString, Repository.class);
        } catch (JsonSyntaxException ex) {
            throw new MojoExecutionException("ERROR: Failed to parse repository json " + repositoryAsString, ex);
        }

        repository.drivers = sortDrivers(repository.drivers);

        return repository;
    }

    private static List<Driver> sortDrivers(List<Driver> drivers) {
        Comparator byId = new ArgumentComparator(on(Driver.class).getId());
        Comparator byVersion = new ArgumentComparator(on(Driver.class).getVersion());
        Comparator orderByIdAndVersion = ComparatorUtils.chainedComparator(byId, byVersion);

        return sort(drivers, on(Driver.class), orderByIdAndVersion);
    }

    private static String downloadAsString(URL url, Proxy proxySettings) throws IOException {
        URLConnection connection;
        if (proxySettings != null) {
            java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP,
                    new InetSocketAddress(proxySettings.getHost(), proxySettings.getPort()));
            if (proxySettings.getUsername() != null) {
                ProxyUtils.setProxyAuthenticator(proxySettings);
            }
            connection = url.openConnection(proxy);
        } else {
            connection = url.openConnection();
        }

        try (InputStream inputStream = connection.getInputStream()) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }

    public List<Driver> getDrivers() {
        return drivers;
    }

    public List<Driver> getDrivers(String name, String platform, String bit, String version) {
        List<Driver> drivers = this.drivers;
        if (name != null) {
            drivers = select(drivers, having(on(Driver.class).getName(), is(equalToIgnoringCase(name))));
        }
        if (platform != null) {
            drivers = select(drivers, having(on(Driver.class).getPlatform(), is(equalToIgnoringCase(platform))));
        }
        if (bit != null) {
            drivers = select(drivers, having(on(Driver.class).getBit(), is(equalToIgnoringCase(bit))));
        }
        if (version != null) {
            drivers = select(drivers, having(on(Driver.class).getComparableVersion(), is(new ComparableVersion(version))));
        }
        return drivers;
    }

    public Driver enrichDriver(Driver driver) throws MojoExecutionException {
        if (isBlank(driver.getName())) {
            throw new MojoExecutionException("Driver name must be set, driver = " + driver.toString());
        }
        if (isNotBlank(driver.getUrl())) {
            return driver;
        }
        if (isNotBlank(driver.getPlatform()) || isNotBlank(driver.getBit()) || isNotBlank(driver.getVersion())) {
            // Explicit driver config make sure it exists in repo
            if (getDrivers(driver.getName(), driver.getPlatform(), driver.getBit(), driver.getVersion()).size() == 0) {
                throw new MojoExecutionException("Could not find driver: " + driver);
            }
        }

        if (isBlank(driver.getPlatform())) {
            String platform;
            if (isMac()) {
                platform = "mac";
            } else if (isLinux()) {
                platform = "linux";
            } else {
                platform = "windows";
            }
            driver.setPlatform(platform);
        }
        if (isBlank(driver.getBit())) {
            String bit;
            if (isLinux() && is64Bit()) {
                bit = "64";
            } else {
                bit = "32";
            }
            driver.setBit(bit);
        }
        if (isBlank(driver.getVersion())) {
            driver.setVersion(getLatestDriverVersion(driver.getId()));
        }

        try {
            return getDrivers(driver.getName(), driver.getPlatform(), driver.getBit(), driver.getVersion()).get(0);
        } catch (IndexOutOfBoundsException ex) {
            // Could not find any driver for the current platform/bit/version in repo
            return null;
        }
    }

    public List<Driver> getLatestDrivers() {
        List<Driver> latestDrivers = new ArrayList<Driver>();
        Collection<String> driverNames = selectDistinct(collect(drivers, on(Driver.class).getName()));

        String platform;
        if (isMac()) {
            platform = "mac";
        } else if (isLinux()) {
            platform = "linux";
        } else {
            platform = "windows";
        }

        String bit;
        if (isLinux() && is64Bit()) {
            bit = "64";
        } else {
            bit = "32";
        }

        for (String driverName : driverNames) {
            List<Driver> driversWithDriverName = select(drivers, having(on(Driver.class).getName(), is(driverName)));
            List<Driver> driversWithDriverNameAndPlatform = select(driversWithDriverName, having(on(Driver.class).getPlatform(), is(platform)));
            List<Driver> driversWithDriverNameAndPlatformAndBit = select(driversWithDriverNameAndPlatform, having(on(Driver.class).getBit(), is(bit)));
            Driver latestDriver = selectMax(driversWithDriverNameAndPlatformAndBit, on(Driver.class).getComparableVersion());
            if (latestDriver != null) {
                latestDrivers.add(latestDriver);
            }
        }

        return sortDrivers(latestDrivers);
    }

    public String getLatestDriverVersion(String driverId) {
        List<Driver> allDriverVersions = select(drivers, having(on(Driver.class).getId(), is(driverId)));
        Driver latestDriver = selectMax(allDriverVersions, on(Driver.class).getComparableVersion());
        if (latestDriver == null) {
            return null;
        }
        return latestDriver.getVersion();
    }

}
