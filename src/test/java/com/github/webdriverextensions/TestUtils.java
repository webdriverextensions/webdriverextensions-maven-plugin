package com.github.webdriverextensions;


import org.junit.Assert;

import java.io.File;

public class TestUtils {

    public static void assertDriverIsInstalled(String driverFileName, File installationDirectory) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        for (File file : installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(driverFileName + ".version")) {
                foundDriverVersionFile = true;
            }
        }
        if (!foundDriverFile) {
            Assert.fail("Driver with file name " + driverFileName + " was not found in the installation directory");
        }
        if (!foundDriverVersionFile) {
            Assert.fail("Driver version file with file name " + driverFileName + ".version was not found in the installation directory");
        }
    }

    public static void assertDriverIsNotInstalled(String driverFileName, File installationDirectory) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        for (File file : installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(driverFileName + ".version")) {
                foundDriverVersionFile = true;
            }
        }
        if (foundDriverFile) {
            Assert.fail("Driver with file name " + driverFileName + " was found in the installation directory when it should not have been");
        }
        if (foundDriverVersionFile) {
            Assert.fail("Driver version file with file name " + driverFileName + ".version was not found in the installation directory when it should not have been");
        }
    }
}
