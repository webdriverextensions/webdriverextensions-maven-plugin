package com.github.webdriverextensions;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.Data;

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

    public String getId() {

        if (customFileName != null && !customFileName.isEmpty()) {
            return customFileName;
        }

        return name + (platform != null ? "-" + platform : "") + (bit != null ? "-" + bit + "bit" : "");
    }

    public String getDriverDownloadDirectoryName() {
        return name
                + (platform != null ? "-" + platform : "")
                + (bit != null ? "-" + bit + "bit" : "")
                + (version != null ? "-" + version : "");
    }

    public ComparableVersion getComparableVersion() {
        return new ComparableVersion(version);
    }

    public String getFileName() {
        if ("windows".equalsIgnoreCase(platform)) {
            return getId() + ".exe";
        } else {
            return getId();
        }
    }

    public String getFilenameFromUrl() {
        try {
            String file = new URL(url).getFile();
            return file.replaceAll(".*\\/", "");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    static Driver fromJson(String json) {
        try {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Driver.class);
        } catch (Exception ex) {
            return new Driver();
        }
    }
}
