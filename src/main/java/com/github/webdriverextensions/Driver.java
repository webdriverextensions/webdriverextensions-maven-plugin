package com.github.webdriverextensions;

import com.google.gson.Gson;

public class Driver {

    private String name;
    private String platform;
    private String bit;
    private String version;
    private String url;
    private String checksum;
    private String fileMatchInside;

    public String getId() {
        return name
                + (platform != null ? "-" + platform : "")
                + (bit != null ? "-" + bit + "bit" : "");
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

    public String getChecksum() {
        return checksum;
    }

    public String getFileName() {
        if ("windows".equalsIgnoreCase(platform)) {
            return getId() + ".exe";
        } else {
            return getId();
        }
    }

    public String getFileMatchInside() {
        return fileMatchInside;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
