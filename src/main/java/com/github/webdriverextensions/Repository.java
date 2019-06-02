package com.github.webdriverextensions;

import static ch.lambdaj.Lambda.*;
import static com.github.webdriverextensions.Utils.*;
import static org.apache.commons.lang3.CharEncoding.UTF_8;
import static org.apache.commons.lang3.StringUtils.*;
import static org.hamcrest.Matchers.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import com.google.gson.*;

import ch.lambdaj.function.compare.ArgumentComparator;

class Repository {

    private List<Driver> drivers;

    static Repository load(URL repositoryUrl, Proxy proxySettings) throws MojoExecutionException {
        String repositoryAsString;
        try {
            repositoryAsString = downloadAsString(repositoryUrl, proxySettings);
        } catch (IOException e) {
            throw new InstallDriversMojoExecutionException("Failed to download repository from url " + quote(
                    repositoryUrl), e);
        }

        Repository repository;
        try {
            repository = new Gson().fromJson(repositoryAsString, Repository.class);
        } catch (JsonSyntaxException e) {
            throw new InstallDriversMojoExecutionException("Failed to parse repository json " + repositoryAsString, e);
        }

        repository.drivers = sortDrivers(repository.drivers);

        return repository;
    }

    @SuppressWarnings("unchecked")
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
                                                      new InetSocketAddress(proxySettings.getHost(),
                                                                            proxySettings.getPort()));
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

    List<Driver> getDrivers(String name, String platform, String bit, String version) {
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
            drivers = select(drivers,
                             having(on(Driver.class).getComparableVersion(), is(new ComparableVersion(version))));
        }
        return drivers;
    }

    Driver enrichDriver(Driver driver) throws MojoExecutionException {
        if (isBlank(driver.getName())) {
            throw new InstallDriversMojoExecutionException("Driver name must be set in configuration, driver: " + driver);
        }
        if (isNotBlank(driver.getUrl())) {
            return driver;
        }
        if (isNotBlank(driver.getPlatform()) || isNotBlank(driver.getBit()) || isNotBlank(driver.getVersion())) {
            // Explicit driver config make sure it exists in repo
            if (getDrivers(driver.getName(), driver.getPlatform(), driver.getBit(), driver.getVersion()).size() == 0) {
                throw new MojoExecutionException("Could not find driver: " + driver + System.lineSeparator()
                                                 + System.lineSeparator()
                                                 + "in repository: " + this);
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

        List<Driver> drivers = getDrivers(driver.getName(),
                                          driver.getPlatform(),
                                          driver.getBit(),
                                          driver.getVersion());
        if (drivers.isEmpty()) {
            if ("64".equals(driver.getBit())) {
                // toogle bits and try the other bit to get a driver configuration
                drivers = getDrivers(driver.getName(),
                                     driver.getPlatform(),
                                     "32",
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

        if (isNotBlank(driver.getCustomFileName()))
        {
            foundDriver.setCustomFileName(driver.getCustomFileName());
        }

        return foundDriver;
    }

    List<Driver> getLatestDrivers() {
        List<Driver> latestDrivers = new ArrayList<Driver>();
        Collection<String> driverNames = selectDistinct(collect(drivers, on(Driver.class).getName()));

        String platform = detectPlatform();

        for (String driverName : driverNames) {
            List<Driver> driversWithDriverName = select(drivers, having(on(Driver.class).getName(), is(driverName)));
            List<Driver> driversWithDriverNameAndPlatform = select(driversWithDriverName,
                                                                   having(on(Driver.class).getPlatform(),
                                                                          is(platform)));
            String bit = detectBits(driverName);
            boolean is64Bit = bit.equals("64");
            Driver latestDriver = getDriverByBit(bit, driversWithDriverNameAndPlatform);
            if (latestDriver != null) {
                latestDrivers.add(latestDriver);
            } else if (is64Bit) {
                Driver latestDriverComplement = getDriverByBit("32", driversWithDriverNameAndPlatform);
                if (latestDriverComplement != null) {
                    latestDrivers.add(latestDriverComplement);
                }
            }
        }

        return sortDrivers(latestDrivers);
    }

    private Driver getDriverByBit(String bit, List<Driver> driversWithDriverNameAndPlatform) {
        List<Driver> driversWithDriverNameAndPlatformAndBit = select(driversWithDriverNameAndPlatform,
                                                                     having(on(Driver.class).getBit(), is(bit)));

        return selectMax(driversWithDriverNameAndPlatformAndBit,
                         on(Driver.class).getComparableVersion());
    }

    private static String detectBits(String driverName) {
        // Default installed internetexplorer bit version on < Windows 10 versions is 32 bit
        if (driverName.equals("internetexplorerdriver") && !isWindows10()) {
            return "32";
        }

        // Detect bit version from os
        if (is64Bit()) {
            return "64";
        }
        return "32";
    }

    private static String detectPlatform() {
        if (isMac()) {
            return "mac";
        } else if (isLinux()) {
            return "linux";
        }
        return "windows";
    }

    private String getLatestDriverVersion(String driverId) {
        List<Driver> allDriverVersions = select(drivers, having(on(Driver.class).getId(), is(driverId)));
        Driver latestDriver = filterLatestDriver(allDriverVersions);
        if (latestDriver == null) {
            return null;
        }
        return latestDriver.getVersion();
    }

    private Driver filterLatestDriver(List<Driver> drivers) {
        return selectMax(drivers, on(Driver.class).getComparableVersion());
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
