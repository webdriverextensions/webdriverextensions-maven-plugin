package com.github.webdriverextensions;

import java.util.Optional;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;

class ProxyUtils {

    private ProxyUtils() {
    }

    static Optional<HttpHost> createProxyFromSettings(Proxy proxySettings) {
        return Optional.ofNullable(proxySettings).map(settings -> new HttpHost(settings.getHost(), settings.getPort()));
    }

    static Optional<CredentialsProvider> createProxyCredentialsFromSettings(Proxy proxySettings) {
        return Optional.ofNullable(proxySettings)
                .filter(settings -> settings.getUsername() != null || !settings.getUsername().isEmpty())
                .map(settings -> {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(settings.getHost(), settings.getPort()), new UsernamePasswordCredentials(settings.getUsername(), settings.getPassword().toCharArray()));
                    return credentialsProvider;
                });
    }

    static Optional<Proxy> getProxyFromSettings(Settings settings, String preferredProxyId) {
        if (settings == null) {
            return Optional.empty();
        }

        return settings.getProxies().stream()
                .filter(Proxy::isActive)
                .filter(proxy -> preferredProxyId != null ? preferredProxyId.equals(proxy.getId()) : true)
                .filter(proxy -> "http".equalsIgnoreCase(proxy.getProtocol()) || "https".equalsIgnoreCase(proxy.getProtocol()))
                .findFirst();
    }
}
