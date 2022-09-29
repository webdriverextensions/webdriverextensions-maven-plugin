package com.github.webdriverextensions;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.codehaus.plexus.util.StringUtils;
import org.openqa.selenium.Platform;

@UtilityClass
public class Utils {

    public static final String FAKED_OS_NAME_PROPERTY_KEY = "webdriverextensions.faked.os.name";
    public static final String FAKED_BIT_PROPERTY_KEY = "webdriverextensions.faked.bit";

    public static String quote(String text) {
        return "\"" + text + "\"";
    }

    public static String quote(Path path) {
        return quote(path.toString());
    }

    public static String quote(URL url) {
        return quote(url.toString());
    }

    public static boolean isWindows() {
        if (System.getProperty(FAKED_OS_NAME_PROPERTY_KEY) != null) {
            return System.getProperty(FAKED_OS_NAME_PROPERTY_KEY).equals("windows");
        }
        return Platform.getCurrent().is(Platform.WINDOWS);
    }

    public static boolean isWindows10() {
        if (System.getProperty(FAKED_OS_NAME_PROPERTY_KEY) != null) {
            return System.getProperty(FAKED_OS_NAME_PROPERTY_KEY).equals("windows10");
        }
        return Platform.getCurrent().is(Platform.WIN10);
    }

    public static boolean isMac() {
        if (System.getProperty(FAKED_OS_NAME_PROPERTY_KEY) != null) {
            return System.getProperty(FAKED_OS_NAME_PROPERTY_KEY).equals("mac");
        }
        return Platform.getCurrent().is(Platform.MAC);
    }

    public static boolean isLinux() {
        if (System.getProperty(FAKED_OS_NAME_PROPERTY_KEY) != null) {
            return System.getProperty(FAKED_OS_NAME_PROPERTY_KEY).equals("linux");
        }
        return Platform.getCurrent().is(Platform.LINUX);
    }

    public static String detectPlatform() {
        if (isMac()) {
            return "mac";
        } else if (isLinux()) {
            return "linux";
        }
        return "windows";
    }

    public static boolean is64Bit() {
        if (System.getProperty(FAKED_BIT_PROPERTY_KEY) != null) {
            return "64".equalsIgnoreCase(System.getProperty(FAKED_BIT_PROPERTY_KEY));
        }
        return "64".equalsIgnoreCase(System.getProperty("sun.arch.data.model"));
    }

    public static String debugInfo(InstallDriversMojo mojo) {
        return System.lineSeparator()
                + "downloadDirectory: " + System.lineSeparator() + directoryToString(mojo.downloadDirectory) + System.lineSeparator()
                + "tempDirectory: " + System.lineSeparator() + directoryToString(mojo.tempDirectory) + System.lineSeparator()
                + "installationDirectory: " + System.lineSeparator() + directoryToString(mojo.installationDirectory.toPath());
    }

    public static String debugInfo(Driver driver) {
        return System.lineSeparator()
                + "driver: " + driver;
    }

    public static String debugInfo(InstallDriversMojo mojo, Driver driver) {
        return System.lineSeparator() + System.lineSeparator()
                + "driver: " + driver + System.lineSeparator() + System.lineSeparator()
                + "downloadDirectory: " + System.lineSeparator() + directoryToString(mojo.downloadDirectory) + System.lineSeparator()
                + "tempDirectory: " + System.lineSeparator() + directoryToString(mojo.tempDirectory) + System.lineSeparator()
                + "installationDirectory: " + System.lineSeparator() + directoryToString(mojo.installationDirectory.toPath());
    }

    public static String directoryToString(Path path) {
        if (path == null) {
            return "null";
        }
        if (!path.toFile().exists()) {
            return path + " does not exist" + System.lineSeparator();
        }
        if (!path.toFile().isDirectory()) {
            throw new IllegalArgumentException("The path is not a directory: " + path);
        }

        File[] files = path.toFile().listFiles();
        if (files.length == 0) {
            return path + " is empty" + System.lineSeparator();
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(path);
        stringBuilder.append(System.lineSeparator());

        int padSize = longestPath(files, path);
        for (int i = 0, l = files.length; i < l; i++) {
            File file = files[i];
            String relativePath = getRelativePath(file, path);
            if (i == l - 2) {
                stringBuilder.append("├── ");
            } else {
                stringBuilder.append("└── ");
            }
            stringBuilder.append(StringUtils.rightPad(relativePath, padSize));
            stringBuilder.append(readableFileSize(file));
            stringBuilder.append(System.lineSeparator());
        }

        return stringBuilder.toString();
    }

    private static int longestPath(File[] files, Path relativeToPath) {
        return Stream.of(files)
                .mapToInt(file -> getRelativePath(file, relativeToPath).length())
                .max().orElse(0);
    }

    private static String getRelativePath(File file, Path relativeToPath) {
        return relativeToPath.relativize(file.toPath()).toString();
    }

    private static String readableFileSize(File file) {
        long size = file.length();
        final String[] units = new String[]{"B", "KiB", "MiB", "GiB", "TiB"};
        int digitGroups = size > 0 ? (int) (Math.log10(size) / Math.log10(1024)) : 0;
        return String.format("%8s %s", new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)), units[digitGroups]);
    }
}
