package com.github.webdriverextensions;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

class ProxyUtils {

    private ProxyUtils() {
    }
    
    static HttpHost createProxyFromSettings(Proxy proxySettings) {
        if (proxySettings == null) {
            return null;
        }
        return new HttpHost(proxySettings.getHost(), proxySettings.getPort());
    }

    static CredentialsProvider createProxyCredentialsFromSettings(Proxy proxySettings, HttpHost proxy) {
        if (proxySettings.getUsername() == null) {
            return null;
        }
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(proxySettings.getUsername(), proxySettings.getPassword().toCharArray()));

        return credentialsProvider;
    }

    static Proxy getProxyFromSettings(InstallDriversMojo mojo) throws MojoExecutionException {
        if (mojo.settings == null) {
            return null;
        }

        if (mojo.proxyId != null) {
            for (Proxy proxy : mojo.settings.getProxies()) {
                if (mojo.proxyId.equals(proxy.getId())) {
                    return proxy;
                }
            }
            throw new InstallDriversMojoExecutionException("Configured proxy with id=" + mojo.proxyId + " not found in settings.xml");
        }

        // Get active http/https proxy
        for (Proxy proxy : mojo.settings.getProxies()) {
            if (proxy.isActive() && ("http".equalsIgnoreCase(proxy.getProtocol()) || "https".equalsIgnoreCase(proxy.getProtocol()))) {
                return proxy;
            }
        }

        return null;
    }
}
