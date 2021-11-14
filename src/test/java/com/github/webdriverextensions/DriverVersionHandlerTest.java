/*
 * Copyright 2021 WebDriver Extensions.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.webdriverextensions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DriverVersionHandlerTest {

    private Driver createDummy() {
        Driver driver = new Driver();
        driver.setBit("42");
        driver.setName("test");
        driver.setPlatform("test-platform");
        driver.setUrl("foo://bar.tld");
        driver.setVersion("1.2.3-alpha01");
        return driver;
    }

    @Test
    void testWriteFileWithNonExistingFile(@TempDir Path tempFolder) {
        Driver driver = createDummy();

        DriverVersionHandler uut = new DriverVersionHandler(tempFolder);
        assertThatCode(() -> uut.writeVersionFile(driver)).doesNotThrowAnyException();

        assertThat(tempFolder.resolve(driver.getId() + ".version")).content(StandardCharsets.UTF_8).isEqualToIgnoringNewLines(driver.toString());
    }

    @Test
    void testWriteFileWithExistingFile(@TempDir Path tempFolder) throws IOException {
        Driver driver = createDummy();
        Files.write(tempFolder.resolve(driver.getId() + ".version"), "test".getBytes(StandardCharsets.UTF_8));

        DriverVersionHandler uut = new DriverVersionHandler(tempFolder);
        assertThatCode(() -> uut.writeVersionFile(driver)).doesNotThrowAnyException();
        final String toString = driver.toString();

        assertThat(tempFolder.resolve(driver.getId() + ".version")).content(StandardCharsets.UTF_8).isEqualToIgnoringNewLines(toString);
    }

    @Test
    void testWriteFileWithExistingReadonlyFile(@TempDir Path tempFolder) throws Exception {
        Driver driver = createDummy();
        final Path exisitingFile = tempFolder.resolve(driver.getId() + ".version");
        Files.write(exisitingFile, "test".getBytes(StandardCharsets.UTF_8));
        exisitingFile.toFile().setReadOnly();

        DriverVersionHandler uut = new DriverVersionHandler(tempFolder);
        assertThatCode(() -> uut.writeVersionFile(driver)).isInstanceOf(InstallDriversMojoExecutionException.class).hasCauseInstanceOf(IOException.class);

        exisitingFile.toFile().setWritable(true);
        assertThat(exisitingFile).content(StandardCharsets.UTF_8).isEqualTo("test");
    }

    @Test
    void testIsSameVersionWithNonExistingFile(@TempDir Path tempFolder) throws MojoExecutionException {
        Driver driver = createDummy();
        DriverVersionHandler uut = new DriverVersionHandler(tempFolder);
        assertThat(uut.isSameVersion(driver)).isFalse();
    }

    @Test
    void testIsSameVersionWithExistingFile(@TempDir Path tempFolder) throws Exception {
        Driver driver = createDummy();
        Files.write(tempFolder.resolve(driver.getId() + ".version"), "{\"name\":\"test\",\"platform\":\"test-platform\",\"bit\":\"42\",\"version\":\"1.2.3-alpha01\",\"url\":\"foo://bar.tld\"}".getBytes(StandardCharsets.UTF_8));
        DriverVersionHandler uut = new DriverVersionHandler(tempFolder);
        assertThat(uut.isSameVersion(driver)).isTrue();
    }

    @Test
    void testIsSameVersionWithExistingButInvalidFile(@TempDir Path tempFolder) throws Exception {
        Driver driver = createDummy();
        Files.write(tempFolder.resolve(driver.getId() + ".version"), "test".getBytes(StandardCharsets.UTF_8));
        DriverVersionHandler uut = new DriverVersionHandler(tempFolder);
        assertThat(uut.isSameVersion(driver)).isFalse();
    }
}
