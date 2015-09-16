package com.github.webdriverextensions;

import com.google.common.collect.ImmutableList;
import java.net.URL;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MagicByteFileTypeDetectorTest {
    private final static String DIR = "/testfiles/";

    private final String filename;
    private final String expectedMimeType;

    public MagicByteFileTypeDetectorTest(String filename, String expectedMimeType) {
        this.filename = filename;
        this.expectedMimeType = expectedMimeType;
    }

    @Parameterized.Parameters(name = "test {0} to be {1}")
    public static List<Object[]> createDataSet() {
        return ImmutableList.of(
                new Object[]{"data.tar", "application/x-tar"},
                new Object[]{"data.tar.bz2", "application/x-bzip2"},
                new Object[]{"data.tar.gz", "application/gzip"},
                new Object[]{"data.tar.xz", "application/x-xz"},
                new Object[]{"data.zip", "application/zip"},
                new Object[]{"echo", "application/octet-stream"});
    }

    @Test
    public void test() {
        String name = DIR + filename;
        String file = getFile(name);
        String mimeType = new MagicByteFileTypeDetector().detectFileType(file);
        assertThat(mimeType).isEqualTo(expectedMimeType);
    }

    private String getFile(String name) {
        URL resource = getClass().getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException("path is broken for :" + name);
        }
        return resource.getFile();
    }
}