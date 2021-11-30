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
location, see the [section below](#installing-a-driver-from-an-url) for
more details on how.

There are some issues with the tests failing to download the phantomjs driver from bitbucket. This should however only occur in the tests.

## Usage
### Installing the Latest Drivers
To install the latest drivers for the current platform and the most probable bit
version add the plugin configured to execute the install-drivers goal.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
The installed driver's bit version will be 32 bit if you are running the plugin
from a windows or mac platform. However on linux platforms the OS bit version
will determine the bit version of the installed driver.

Note that the plugin will automatically update the driver if a newer driver
version is released.

### Installing Specific Drivers
If you wish to be more specific about which drivers to install you can provide
the drivers yourself in the plugin configuration.

E.g. to install specific versions of all available drivers
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <drivers>
            <driver>
                <name>internetexplorerdriver</name>
                <platform>windows</platform>
                <bit>32</bit>
                <version>3.9.0</version>
            </driver>
            <driver>
                <name>internetexplorerdriver</name>
                <platform>windows</platform>
                <bit>64</bit>
                <version>3.9.0</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>windows</platform>
                <bit>32</bit>
                <version>74.0.3729.6</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>mac</platform>
                <bit>64</bit>
                <version>74.0.3729.6</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>linux</platform>
                <bit>64</bit>
                <version>74.0.3729.6</version>
            </driver>
            <driver>
                <name>phantomjs</name>
                <platform>windows</platform>
                <bit>64</bit>
                <version>2.1.1</version>
            </driver>
            <driver>
                <name>phantomjs</name>
                <platform>mac</platform>
                <bit>64</bit>
                <version>2.1.1</version>
            </driver>
            <driver>
                <name>phantomjs</name>
                <platform>linux</platform>
                <bit>32</bit>
                <version>2.1.1</version>
            </driver>
            <driver>
                <name>phantomjs</name>
                <platform>linux</platform>
                <bit>64</bit>
                <version>2.1.1</version>
            </driver>
            <driver>
                <name>geckodriver</name>
                <platform>windows</platform>
                <bit>64</bit>
                <version>0.24.0</version>
            </driver>
            <driver>
                <name>geckodriver</name>
                <platform>windows</platform>
                <bit>32</bit>
                <version>0.24.0</version>
            </driver>
            <driver>
                <name>geckodriver</name>
                <platform>mac</platform>
                <bit>64</bit>
                <version>0.24.0</version>
            </driver>
            <driver>
                <name>geckodriver</name>
                <platform>linux</platform>
                <bit>64</bit>
                <version>0.24.0</version>
            </driver>
            <driver>
                <name>geckodriver</name>
                <platform>linux</platform>
                <bit>32</bit>
                <version>0.24.0</version>
            </driver>
            <driver>
                <name>edgedriver</name>
                <platform>windows</platform>
                <bit>64</bit>
                <version>6.17134</version>
            </driver>
            <driver>
                <name>operadriver</name>
                <platform>windows</platform>
                <bit>32</bit>
                <version>2.30</version>
            </driver>
            <driver>
                <name>operadriver</name>
                <platform>windows</platform>
                <bit>64</bit>
                <version>2.30</version>
            </driver>
            <driver>
                <name>operadriver</name>
                <platform>mac</platform>
                <bit>64</bit>
                <version>2.30</version>
            </driver>
            <driver>
                <name>operadriver</name>
                <platform>linux</platform>
                <bit>64</bit>
                <version>2.30</version>
            </driver>
        </drivers>
    </configuration>
</plugin>
```

If you wish to make sure that you always have the latest driver installed omit
the version of the driver. For more detailed driver configuration possibilities
see the [plugin goal documentation](http://webdriverextensions.github.io/webdriverextensions-maven-plugin/install-drivers-mojo.html#drivers).

### Installing a Driver from an URL
If the driver is not available in the
[default drivers repository-3.0.json file](https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository/blob/master/repository-3.0.json) you can install the driver by also
providing an URL to the download location.

E.g. to install an old Chrome Driver for windows
```xml
<driver>
    <name>chromedriver</name>
    <platform>windows</platform>
    <bit>32</bit>
    <version>2.27</version>
    <url>http://chromedriver.storage.googleapis.com/2.27/chromedriver_win32.zip</url>
</driver>
```

### Selecting files to extract
When installing a custom driver you can select what files should be extracted from 
the downloaded zip/bz2 file. This is done by providing a regex pattern in a tag named
`<fileMatchInside>`.
```xml
<driver>
    <name>phantomjs</name>
    <platform>linux</platform>
    <bit>32</bit>
    <version>2.1.1</version>
    <url>https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.6-linux-i686.tar.bz2</url>
    <fileMatchInside>.*/bin/phantomjs$</fileMatchInside>
</driver>
```

### Changing the Installation Directory
By default the drivers are installed a directory called `drivers` in the maven
project root (`${basedir}/drivers`). To install the drivers to another directory specify the custom
path through the configuration parameter named `installationDirectory`.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <installationDirectory>/Users/andidev/drivers</installationDirectory>
        <drivers>
            ... drivers to install
        </drivers>
    </configuration>
</plugin>
```

### Keeping downloaded data in the cache
To avoid downloading the drivers more than once if you switch between 
driver versions or something similar you could set `<keepDownloadedWebdrivers>true</keepDownloadedWebdrivers>` configuration paramter. Please note that you must also provide a fixed path for `pluginWorkingDirectory` otherwise `keepDownloadedWebdrivers=true` will be ignored!
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <keepDownloadedWebdrivers>true</keepDownloadedWebdrivers>
        <pluginWorkingDirectory>/some/directory</pluginWorkingDirectory>
        <drivers>
            ... drivers to install
        </drivers>
    </configuration>
</plugin>
```

### Using a proxy
If you have configured a proxy in the settings.xml file the first encountered active proxy
will be used. To specify a specific proxy to use you can provide the proxy id
in the configuration.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <proxyId>yourproxyid</proxyId>
    </configuration>
</plugin>
```

### Skipping the driver installation 
To skip the installation you can add `<skip>true</skip>` in the configuration tag.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <skip>true</skip>
        <drivers>
            ... drivers to install
        </drivers>
    </configuration>
</plugin>
```

### Setting the driver filename
To set a custom file name for the specified driver configuration.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <drivers>
            <driver>
                <name>chromedriver</name>
                <platform>linux</platform>
                <customFileName>chrome-linux-custom</customFileName>
            </driver>
        </drivers>
    </configuration>
</plugin>
```

### Setting a custom repository URL
To set a custom URL to the JSON repository file. The supported URL schemes are: http, https and file. The content of the file must validate against [the drivers repository JSON schema](drivers-schema.json).

**Example for local file**
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <repositoryUrl>file:///some/path/to/repo.json</repositoryUrl>
    </configuration>
</plugin>
```

### Further Configurations
For more details on how to further configure this plugin please see the
[plugin goal documentation](http://webdriverextensions.github.io/webdriverextensions-maven-plugin/install-drivers-mojo.html).



## Changelog
Please refer to [CHANGELOG.md](CHANGELOG.md) for a list of changes.

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
