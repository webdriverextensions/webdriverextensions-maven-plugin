package com.github.webdriverextensions;

import static com.github.webdriverextensions.Utils.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.TransformerUtils;
import org.apache.commons.collections4.functors.TransformedPredicate;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

import com.google.gson.*;

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

    private static List<Driver> sortDrivers(List<Driver> drivers) {
        Comparator<Driver> byId = new DriverComparator.ById();
        // sort by version descending (newest first)
        Comparator<Driver> byVersion = ComparatorUtils.reversedComparator(new DriverComparator.ByVersion());
        @SuppressWarnings("unchecked")
        Comparator<Driver> orderByIdAndVersion = ComparatorUtils.chainedComparator(byId, byVersion);

        List<Driver> listToSort = new ArrayList<>(drivers);
        Collections.sort(listToSort, orderByIdAndVersion);
        return listToSort;
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
        return filterDrivers(drivers, name, platform, bit, version);
    }

    @SuppressWarnings("unchecked")
    private List<Driver> filterDrivers(List<Driver> driversToFilter, String name, String platform, String bit, String version) {
        List<Driver> filteredDrivers = new ArrayList<>(driversToFilter);
        Class<?>[] toLowerArgTypes = {Locale.class};
        Object[] toLowerArgs = {Locale.ROOT};
        if (name != null) {
            CollectionUtils.filter(filteredDrivers, new TransformedPredicate(
                    TransformerUtils.chainedTransformer(TransformerUtils.invokerTransformer("getName"), TransformerUtils.invokerTransformer("toLowerCase", toLowerArgTypes, toLowerArgs)),
                    PredicateUtils.equalPredicate(name.toLowerCase(Locale.ROOT)))
            );
        }
        if (platform != null) {
            CollectionUtils.filter(filteredDrivers, new TransformedPredicate(
                    TransformerUtils.chainedTransformer(TransformerUtils.invokerTransformer("getPlatform"), TransformerUtils.invokerTransformer("toLowerCase", toLowerArgTypes, toLowerArgs)),
                    PredicateUtils.equalPredicate(platform.toLowerCase(Locale.ROOT)))
            );
        }
        if (bit != null) {
            CollectionUtils.filter(filteredDrivers, new TransformedPredicate(
                    TransformerUtils.chainedTransformer(TransformerUtils.invokerTransformer("getBit"), TransformerUtils.invokerTransformer("toLowerCase", toLowerArgTypes, toLowerArgs)),
                    PredicateUtils.equalPredicate(bit.toLowerCase(Locale.ROOT)))
            );
        }
        if (version != null) {
            CollectionUtils.filter(filteredDrivers, new TransformedPredicate(
                    TransformerUtils.invokerTransformer("getComparableVersion"),
                    PredicateUtils.equalPredicate(new ComparableVersion(version)))
            );
        }
        return filteredDrivers;
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
            if (getDrivers(driver.getName(), driver.getPlatform(), driver.getBit(), driver.getVersion()).isEmpty()) {
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
        List<Driver> latestDrivers = new ArrayList<>();
        Set<String> driverNames = new HashSet<>();
        for (Driver driver : drivers) {
            driverNames.add(driver.getName());
        }

        String platform = detectPlatform();

        for (String driverName : driverNames) {
            List<Driver> driversWithDriverNameAndPlatform = getDrivers(driverName, platform, null, null);
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
        List<Driver> driversWithDriverNameAndPlatformAndBit = filterDrivers(driversWithDriverNameAndPlatform, null, null, bit, null);
        return filterLatestDriver(driversWithDriverNameAndPlatformAndBit);
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

    @SuppressWarnings("unchecked")
    private String getLatestDriverVersion(String driverId) {
        List<Driver> allDriverVersions = new ArrayList<>(drivers);
        CollectionUtils.filter(allDriverVersions, new TransformedPredicate(
                TransformerUtils.invokerTransformer("getId"),
                PredicateUtils.equalPredicate(driverId))
        );
        Driver latestDriver = filterLatestDriver(allDriverVersions);
        if (latestDriver == null) {
            return null;
        }
        return latestDriver.getVersion();
    }

    private Driver filterLatestDriver(List<Driver> drivers) {
        List<Driver> sortedDrivers = sortDrivers(drivers);
        return sortedDrivers.size() > 0 ? sortedDrivers.get(0) : null;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
