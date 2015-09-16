package com.github.webdriverextensions;

import java.io.File;
import java.net.URL;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.openqa.selenium.Platform;

public class Utils {
    public static final String quote(String text) {
        return "\"" + text + "\"";
    }

    public static final String quote(File file) {
        return quote(file.getAbsolutePath());
    }

    public static final String quote(URL url) {
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

    public static Proxy getProxyFromSettings(Settings settings, String proxyId) throws MojoExecutionException {
        if (settings == null) {
            return null;
        }

        if (proxyId != null) {
            for (Proxy proxy : settings.getProxies()) {
                if (proxyId.equals(proxy.getId())) {
                    return proxy;
                }
            }
            throw new MojoExecutionException("Configured proxy with id=" + proxyId + " not found in settings.xml");
        }

        // Get active http/https proxy
        for (Proxy proxy : settings.getProxies()) {
            if (proxy.isActive() && ("http".equalsIgnoreCase(proxy.getProtocol()) || "https".equalsIgnoreCase(proxy.getProtocol()))) {
                return proxy;
            }
        }

        return null;
    }

    public static void error(Exception e) throws MojoExecutionException {
        error(e.getMessage(),e);
    }

    private static void error(String message) throws MojoExecutionException {
        throw new MojoExecutionException(message);
    }

    private static void error(String message, Exception e) throws MojoExecutionException {
        throw new MojoExecutionException(message, e);
    }
}
