package com.github.webdriverextensions;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;

public class ProxyUtils {
    public static HttpHost createProxyFromSettings(Proxy proxySettings) throws MojoExecutionException {
        if (proxySettings == null) {
            return null;
        }
        return new HttpHost(proxySettings.getHost(), proxySettings.getPort());
    }

    static CredentialsProvider createProxyCredentialsFromSettings(Proxy proxySettings) throws MojoExecutionException {
        if (proxySettings.getUsername() == null) {
            return null;
        }
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(proxySettings.getUsername(), proxySettings.getPassword()));

        return credentialsProvider;
    }

    public static void setProxyAuthenticator(final Proxy proxy) {
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return (new PasswordAuthentication(proxy.getUsername(),
                        proxy.getPassword().toCharArray()));
            }
        };
        Authenticator.setDefault(authenticator);
    }
}
