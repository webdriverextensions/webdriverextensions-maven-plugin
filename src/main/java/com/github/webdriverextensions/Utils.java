package com.github.webdriverextensions;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Platform;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class Utils {

    public static final String FAKED_OS_NAME_PROPERTY_KEY = "webdriverextensions.faked.os.name";
    public static final String FAKED_BIT_PROPERTY_KEY = "webdriverextensions.faked.bit";

    public static String quote(String text) {
        return "\"" + text + "\"";
    }

    public static String quote(Path path) {
        return quote(path.toString());
    }

    public static String quote(File file) {
        return quote(file.getAbsolutePath());
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

    public static boolean is64Bit() {
        if (System.getProperty(FAKED_BIT_PROPERTY_KEY) != null) {
            switch (System.getProperty(FAKED_BIT_PROPERTY_KEY)) {
                case "64":
                    return true;
                case "32":
                    return false;
            }
        }
        return com.sun.jna.Platform.is64Bit();
    }

    public static String debugInfo(InstallDriversMojo mojo) {
        return System.lineSeparator()
                + "cacheDirectory: " + System.lineSeparator() + directoryToString(mojo.cacheDirectory) + System.lineSeparator()
                + "tempDirectory: " + System.lineSeparator() + directoryToString(mojo.tempDirectory) + System.lineSeparator()
                + "installationDirectory: " + System.lineSeparator() + directoryToString(mojo.installationDirectory);
    }

    public static String debugInfo(Driver driver) {
        return System.lineSeparator()
                + "driver: " + driver;
    }

    public static String debugInfo(InstallDriversMojo mojo, Driver driver) {
        return System.lineSeparator() + System.lineSeparator()
                + "driver: " + driver + System.lineSeparator() + System.lineSeparator()
                + "cacheDirectory: " + System.lineSeparator() + directoryToString(mojo.cacheDirectory) + System.lineSeparator()
                + "tempDirectory: " + System.lineSeparator() + directoryToString(mojo.tempDirectory) + System.lineSeparator()
                + "installationDirectory: " + System.lineSeparator() + directoryToString(mojo.installationDirectory);
    }

    public static String directoryToString(File path) {
        if (!path.exists()) {
            return path + " does not exist" + System.lineSeparator();
        }
        if (!path.isDirectory()) {
            throw new IllegalArgumentException("The path is not a directory: " + path);
        }

        Collection<File> files = FileUtils.listFiles(
                path,
                new RegexFileFilter("^(.*?)"),
                DirectoryFileFilter.DIRECTORY
        );

        if (files.size() == 0) {
            return path + " is empty";
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(path);
        stringBuilder.append(System.lineSeparator());

        int padSize = longestPath(files, path);
        for (Iterator iterator = files.iterator(); iterator.hasNext(); ) {
            File file = (File) iterator.next();
            String relativePath = getRelativePath(file, path);
            if (iterator.hasNext()) {
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

    private static int longestPath(Collection<File> files, File relativeToPath) {
        int max = 0;
        for (File file : files) {
            int currentPathLength = getRelativePath(file, relativeToPath).length();
            if (currentPathLength > max) {
                max = currentPathLength;
            }
        }
        return max;
    }

    private static String getRelativePath(File file, File relativeToPath) {
        return file.getAbsolutePath().replaceFirst(relativeToPath.getAbsolutePath() + File.separator, "");
    }

    private static String readableFileSize(File file) {
        long size = FileUtils.sizeOf(file);
        if (size <= 0) {
            return "0";
        }

        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return StringUtils.leftPad(new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)), 8) + " " + units[digitGroups];
    }

    public static boolean hasExtension(Path downloadFilePath, String zip) {
        return zip.toUpperCase().equals(FilenameUtils.getExtension(downloadFilePath.toString()).toUpperCase());
    }

    public static boolean validateZipFile(Path filePath) {
        ZipFile zipfile = null;
        ZipInputStream zis = null;
        try {
            zipfile = new ZipFile(filePath.toFile());
            zis = new ZipInputStream(new FileInputStream(filePath.toFile()));
            ZipEntry ze = zis.getNextEntry();
            if (ze == null) {
                return false;
            }
            while (ze != null) {
                // if it throws an exception fetching any of the following then we know the file is corrupted.
                zipfile.getInputStream(ze);
                ze.getCrc();
                ze.getCompressedSize();
                ze.getName();
                ze = zis.getNextEntry();
            }
            return true;
        } catch (ZipException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                }
            } catch (IOException e) {
                return false;
            }
            try {
                if (zis != null) {
                    zis.close();
                }
            } catch (IOException e) {
                return false;
            }

        }
    }

    public static boolean validateBz2File(Path filePath) {
        try (FileInputStream fin = new FileInputStream(filePath.toFile())) {
            try (BufferedInputStream bin = new BufferedInputStream(fin)) {
                try (BZip2CompressorInputStream ignored = new BZip2CompressorInputStream(bin)) {
                }
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean validateFileIsLargerThanBytes(Path filePath, int bytes) {
        return FileUtils.sizeOf(filePath.toFile()) > bytes;
    }
}
