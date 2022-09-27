package com.github.webdriverextensions;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.codehaus.plexus.util.StringUtils;
import org.openqa.selenium.Platform;

@UtilityClass
public class Utils {

    public static final String FAKED_OS_NAME_PROPERTY_KEY = "webdriverextensions.faked.os.name";
    public static final String FAKED_ARCH_PROPERTY_KEY = "webdriverextensions.faked.os.arch";
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

    @Nonnull
    public static Architecture detectArch() {
        if (System.getProperty(FAKED_ARCH_PROPERTY_KEY) != null) {
            return Architecture.fromArchName(System.getProperty(FAKED_ARCH_PROPERTY_KEY));
        }
        return Architecture.extractFromSysProperty();
    }

    public static String detectBits() {
        if (System.getProperty(FAKED_BIT_PROPERTY_KEY) != null) {
            return System.getProperty(FAKED_BIT_PROPERTY_KEY);
        }
        final Architecture arch = detectArch();
        switch(arch) {
            case UNKNOWN:
                return System.getProperty("sun.arch.data.model");
            case X86:
                return "32";
            default:
                return "64";
        }
    }

    public static String directoryToString(Path path) {
        if (path == null) {
            return "null";
        }
        if (!path.toFile().exists()) {
            return path + " does not exist";
        }
        if (!path.toFile().isDirectory()) {
            return path + " is not a directory";
        }

        File[] files = path.toFile().listFiles();
        if (files.length == 0) {
            return path + " is empty";
        }

        StringBuilder stringBuilder = new StringBuilder(path.toString());
        int padSize = longestPath(files, path);
        Arrays.sort(files);
        for (int i = 0, l = files.length; i < l; i++) {
            stringBuilder.append(System.lineSeparator());
            File file = files[i];
            String relativePath = getRelativePath(file, path);
            if (i != l - 1) {
                stringBuilder.append("├── ");
            } else {
                stringBuilder.append("└── ");
            }
            stringBuilder.append(StringUtils.rightPad(relativePath, padSize));
            stringBuilder.append(readableFileSize(file));
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
        final String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
        int digitGroups = size > 0 ? (int) (Math.log10(size) / Math.log10(1024)) : 0;
        return String.format("%8s %s", new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)), units[digitGroups]);
    }
}
