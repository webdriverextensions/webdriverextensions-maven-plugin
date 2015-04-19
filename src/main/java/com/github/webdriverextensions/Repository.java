package com.github.webdriverextensions;

import static ch.lambdaj.Lambda.collect;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.Lambda.selectDistinct;
import static ch.lambdaj.Lambda.selectMax;
import static com.github.webdriverextensions.Utils.downloadAsString;
import static com.github.webdriverextensions.Utils.is64Bit;
import static com.github.webdriverextensions.Utils.isLinux;
import static com.github.webdriverextensions.Utils.isMac;
import static com.github.webdriverextensions.Utils.quote;
import static com.github.webdriverextensions.Utils.sortDrivers;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

public class Repository {

    private List<Driver> drivers;

    public static Repository load(URL repositoryUrl, Proxy proxySettings) throws MojoExecutionException {
        String repositoryAsString;
        try {
            repositoryAsString = downloadAsString(repositoryUrl, proxySettings);
        } catch (IOException ex) {
            throw new MojoExecutionException("ERROR: Could not download repository from url " + quote(repositoryUrl), ex);
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

    public Driver getDriver(Driver driver) throws MojoExecutionException {
        if (isBlank(driver.getName())) {
            throw new MojoExecutionException("Driver name must be set, driver = " + driver.toString());
        }
        if (!isBlank(driver.getUrl())) {
            return driver;
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
