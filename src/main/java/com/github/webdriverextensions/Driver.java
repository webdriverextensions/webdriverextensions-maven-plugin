package com.github.webdriverextensions;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.net.MalformedURLException;
import java.net.URL;

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

        if (customFileName!= null)
        {
            if (!customFileName.isEmpty()) {
                return customFileName;
            }
        }

        return name + (platform != null ? "-" + platform : "") + (bit != null ? "-" + bit + "bit" : "");
    }

    public String getDriverDownloadDirectoryName() {
        return name
                + (platform != null ? "-" + platform : "")
                + (bit != null ? "-" + bit + "bit" : "")
                + (version != null ? "-" + version : "");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlatform() {
        return platform;
    }

    public String getCustomFileName() {
        return customFileName;
    }

    public void setCustomFileName(String customFileName) {
        this.customFileName = customFileName;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getBit() {
        return bit;
    }

    public void setBit(String bit) {
        this.bit = bit;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ComparableVersion getComparableVersion() {
        return new ComparableVersion(version);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getFileMatchInside() {
        return fileMatchInside;
    }

    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
