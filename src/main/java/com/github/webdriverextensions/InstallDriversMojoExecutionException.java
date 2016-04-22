package com.github.webdriverextensions;

import org.apache.maven.plugin.MojoExecutionException;

public class InstallDriversMojoExecutionException extends MojoExecutionException {
    public InstallDriversMojoExecutionException(String message) {
        super(message);
    }

    public InstallDriversMojoExecutionException(String message, Exception cause) {
        super(message, cause);
    }

    public InstallDriversMojoExecutionException(String message, InstallDriversMojo mojo, Driver driver) {
        this(message + Utils.debugInfo(mojo, driver));
    }

    public InstallDriversMojoExecutionException(String message, Exception cause, InstallDriversMojo mojo, Driver driver) {
        this(message + Utils.debugInfo(mojo, driver), cause);
    }
}
