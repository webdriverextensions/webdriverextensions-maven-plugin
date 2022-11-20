[![Build Status](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/workflows/CI%20build/badge.svg)](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/actions?query=workflow%3A%22CI+build%22) [![Maven Central](https://img.shields.io/maven-central/v/com.github.webdriverextensions/webdriverextensions-maven-plugin.svg)](https://search.maven.org/artifact/com.github.webdriverextensions/webdriverextensions-maven-plugin)

WebDriver Extensions Maven Plugin
===================

Use this plugin to manage, download and install WebDriver drivers directly from
your pom.

## Available Drivers
The following drivers are currently maintained and available for installation:
- internetexplorerdriver, windows, 32bit
- internetexplorerdriver, windows, 64bit
- chromedriver, windows, 32bit
- chromedriver, mac, 32bit
- chromedriver, mac, 64bit
- chromedriver, linux, 32bit
- chromedriver, linux, 64bit
- phantomjs, windows, 64bit
- phantomjs, mac, 64bit
- phantomjs, linux, 32bit
- phantomjs, linux, 64bit
- geckodriver, windows, 64bit
- geckodriver, mac, 64bit
- geckodriver, linux, 64bit
- edgedriver, windows, 64bit
- operadriver, windows, 32bit
- operadriver, windows, 64bit
- operadriver, mac, 32bit
- operadriver, linux, 32bit
- operadriver, linux, 64bit

We try to update the drivers as soon as we notice they are updated. If you want to help to keep the drivers
updated see the [projects GitHub repository](https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository).
To verify that a version is available for installation check that it exists in the
[default drivers repository-3.0.json file](https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository/blob/master/repository-3.0.json).

However if the driver is not yet available in the repo it can also be installed by providing an URL to the download
location, see the [the plugin usage page](https://webdriverextensions.github.io/webdriverextensions-maven-plugin/usage.html) for
more details on how.

There are some issues with the tests failing to download the phantomjs driver from bitbucket. This should however only occur in the tests.

## Usage
To install the latest drivers for the current platform and the most probable bit
version add the plugin configured to execute the install-drivers goal.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Please refer to [the plugin usage page](https://webdriverextensions.github.io/webdriverextensions-maven-plugin/usage.html) for more examples.

## Changelog
Please refer to [changelog.md](src/site/markdown/changelog.md) for a list of changes.

## License 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
