package com.github.webdriverextensions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {
    @Test
    void directoryToStringTest(@TempDir Path tmp) throws IOException {
        assertThat(Utils.directoryToString(null)).isEqualTo("null");
        assertThat(Utils.directoryToString(tmp)).isEqualTo("%s is empty", tmp);
        assertThat(Utils.directoryToString(tmp.resolve("foo"))).isEqualTo("%s does not exist", tmp.resolve("foo"));
        Path foo = Files.createFile(tmp.resolve("foo"));
        assertThat(Utils.directoryToString(foo)).isEqualTo("%s is not a directory", foo);
        Path bar = Files.createFile(tmp.resolve("bar"));
        Path car = Files.createFile(tmp.resolve("car"));
        assertThat(Utils.directoryToString(tmp)).isEqualTo("%s%n"
                + "├── %s%8s B%n"
                + "├── %s%8s B%n"
                + "└── %s%8s B", tmp, 
                bar.getFileName(), "0", 
                car.getFileName(), "0", 
                foo.getFileName(), "0");
    }
}
