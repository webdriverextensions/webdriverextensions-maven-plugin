package com.github.webdriverextensions;

import lombok.experimental.StandardException;
import org.apache.maven.plugin.MojoExecutionException;

@StandardException
class InstallDriversMojoExecutionException extends MojoExecutionException {

    InstallDriversMojoExecutionException(String message, Driver driver, Throwable cause) {
        super(message, cause);
        this.source = driver;
    }

    void setLongMessage(String longMessage) {
        this.longMessage = longMessage;
    }
}
