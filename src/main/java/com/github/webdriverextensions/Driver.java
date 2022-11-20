package com.github.webdriverextensions;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.Nonnull;
import lombok.Data;
import org.apache.maven.artifact.versioning.ComparableVersion;

@Data
public class Driver {

    @Expose
    private String name;
    @Expose
    private String platform;
    @Expose
    private String bit;
    @Expose
    private String version;
    @Expose
    private String url;
    @Expose
    private String fileMatchInside;
    @Expose
    private String customFileName;
    /**
     * the os/cpu architecture this driver is for. see {@link Architecture} for
     * supported values. if empty, the current detected architecture will be
     * used.
     *
     * @since 3.4.0
     */
    @Expose
    private String arch;

    private transient ComparableVersion comparableVersion;

    @Nonnull
    public String getId() {

        if (customFileName != null && !customFileName.isEmpty()) {
            return customFileName;
        }

        return name + (platform != null ? "-" + platform : "") + (bit != null ? "-" + bit + "bit" : "");
    }

    @Nonnull
    public String getDriverDownloadDirectoryName() {
        return name
                + (platform != null ? "-" + platform : "")
                + (arch != null ? "-" + getArchitecture() : "")
                + (bit != null ? "-" + bit + "bit" : "")
                + (version != null ? "-" + version : "");
    }

    @Nonnull
    public ComparableVersion getComparableVersion() {
        return comparableVersion != null ? comparableVersion : new ComparableVersion(version != null ? version : "");
    }

    @Nonnull
    public String getFileName() {
        if ("windows".equalsIgnoreCase(platform)) {
            return getId() + ".exe";
        } else {
            return getId();
        }
    }

    @Nonnull
    public String getFilenameFromUrl() {
        try {
            String file = new URL(url).getFile();
            return file.replaceAll(".*\\/", "");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nonnull
    public Architecture getArchitecture() {
        return Architecture.fromArchName(arch);
    }

    public void setVersion(String version) {
        this.version = version;
        comparableVersion = new ComparableVersion(version != null ? version : "");
    }

    @Override
    public String toString() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(this);
    }

    @Nonnull
    static Driver fromJson(String json) {
        try {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Driver.class);
        } catch (Exception ex) {
            return new Driver();
        }
    }
}
