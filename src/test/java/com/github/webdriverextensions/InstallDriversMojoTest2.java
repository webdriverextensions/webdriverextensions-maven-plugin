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

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

class InstallDriversMojoTest2 {

    @Test
    @SetSystemProperty(key = "skipTests", value = "true")
    void test_that_skipTests_does_not_install_configured_drivers(@TempDir File tmp) throws MojoExecutionException {
        // Given
        InstallDriversMojo mojo = new InstallDriversMojo();
        mojo.settings = new Settings();
        mojo.installationDirectory = tmp;

        // When
        mojo.execute();

        // Then
        assertThat(mojo.installationDirectory.listFiles()).isNullOrEmpty();
    }
}
