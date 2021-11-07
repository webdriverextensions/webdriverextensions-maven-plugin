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
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class DriverVersionHandlerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
    public void testWriteFileWithNonExistingFile() {
        Driver driver = createDummy();

        DriverVersionHandler uut = new DriverVersionHandler(tempFolder.getRoot().toPath());
        assertThatCode(() -> uut.writeVersionFile(driver)).doesNotThrowAnyException();

        assertThat(tempFolder.getRoot().toPath().resolve(driver.getId() + ".version")).content(StandardCharsets.UTF_8).isEqualToIgnoringNewLines(driver.toString());
    }

    @Test
    public void testWriteFileWithExistingFile() throws IOException {
        Driver driver = createDummy();
        Files.write(tempFolder.newFile(driver.getId() + ".version").toPath(), "test".getBytes(StandardCharsets.UTF_8));

        DriverVersionHandler uut = new DriverVersionHandler(tempFolder.getRoot().toPath());
        assertThatCode(() -> uut.writeVersionFile(driver)).doesNotThrowAnyException();
        final String toString = driver.toString();

        assertThat(tempFolder.getRoot().toPath().resolve(driver.getId() + ".version")).content(StandardCharsets.UTF_8).isEqualToIgnoringNewLines(toString);
    }

    @Test
    public void testIsSameVersionWithNonExistingFile() throws MojoExecutionException {
        Driver driver = createDummy();
        DriverVersionHandler uut = new DriverVersionHandler(tempFolder.getRoot().toPath());
        assertThat(uut.isSameVersion(driver)).isFalse();
    }

    @Test
    public void testIsSameVersionWithExistingFile() throws Exception {
        Driver driver = createDummy();
        Files.write(tempFolder.newFile(driver.getId() + ".version").toPath(), "{\"name\":\"test\",\"platform\":\"test-platform\",\"bit\":\"42\",\"version\":\"1.2.3-alpha01\",\"url\":\"foo://bar.tld\"}".getBytes(StandardCharsets.UTF_8));
        DriverVersionHandler uut = new DriverVersionHandler(tempFolder.getRoot().toPath());
        assertThat(uut.isSameVersion(driver)).isTrue();
    }

    @Test
    public void testIsSameVersionWithExistingButInvalidFile() throws Exception {
        Driver driver = createDummy();
        Files.write(tempFolder.newFile(driver.getId() + ".version").toPath(), "test".getBytes(StandardCharsets.UTF_8));
        DriverVersionHandler uut = new DriverVersionHandler(tempFolder.getRoot().toPath());
        assertThat(uut.isSameVersion(driver)).isFalse();
    }
}
