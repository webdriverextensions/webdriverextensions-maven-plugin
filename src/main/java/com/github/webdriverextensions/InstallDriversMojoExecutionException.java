package com.github.webdriverextensions;

import lombok.experimental.StandardException;
import org.apache.maven.plugin.MojoExecutionException;

@StandardException
public class InstallDriversMojoExecutionException extends MojoExecutionException {
    public InstallDriversMojoExecutionException(String message, InstallDriversMojo mojo, Driver driver) {
        this(message, null, mojo, driver);
    }

    public InstallDriversMojoExecutionException(String message, Exception cause, InstallDriversMojo mojo, Driver driver) {
        this(message + Utils.debugInfo(mojo, driver), cause);
    }
}
