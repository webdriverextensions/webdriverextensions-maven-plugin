package com.github.webdriverextensions;

import java.io.File;
import java.net.URL;
import org.openqa.selenium.Platform;

public class Utils {
    public static String quote(String text) {
        return "\"" + text + "\"";
    }

    public static String quote(File file) {
        return quote(file.getAbsolutePath());
    }

    public static String quote(URL url) {
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
}
