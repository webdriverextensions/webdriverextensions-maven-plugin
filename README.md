[![Build Status](https://travis-ci.org/webdriverextensions/webdriverextensions-maven-plugin.svg?branch=master)](https://travis-ci.org/webdriverextensions/webdriverextensions-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.webdriverextensions/webdriverextensions-maven-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Acom.github.webdriverextensions)

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

The latest driver versions should be available at least one day after the
release of the driver. Since the drivers repository is maintained manually
sometime it may take longer than that. If you want to help to keep the drivers
updated see the [projects GitHub repository](https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository).
To verify that a version is available for installation check that it exists in the
[default drivers repository.json file](https://github.com/webdriverextensions/webdriverextensions-maven-plugin-repository/blob/master/repository.json).

However drivers can also be installed by providing an URL to the download
location, see the [section below](#installing-a-driver-from-an-url) for
more details on how.

## Usage
### Installing the Latest Drivers
To install the latest drivers for the current platform and the most probable bit
version add the plugin configured to execute the install-drivers goal.
```xml
<plugin>
    <groupId>com.github.webdriverextensions</groupId>
    <artifactId>webdriverextensions-maven-plugin</artifactId>
    <version>1.1.0</version>
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
from a windows or mac platform. However on lunux platforms the OS bit version
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
    <version>1.1.0</version>
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
                <version>2.45</version>
            </driver>
            <driver>
                <name>internetexplorerdriver</name>
                <platform>windows</platform>
                <bit>64</bit>
                <version>2.45</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>windows</platform>
                <bit>32</bit>
                <version>2.14</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>mac</platform>
                <bit>32</bit>
                <version>2.14</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>linux</platform>
                <bit>32</bit>
                <version>2.14</version>
            </driver>
            <driver>
                <name>chromedriver</name>
                <platform>linux</platform>
                <bit>64</bit>
                <version>2.14</version>
            </driver>
        </drivers>
    </configuration>
</plugin>
```

If you wish to make sure that you always have the latest driver installed omit
the version of the driver. For more detailed driver configuration possibilities
see the [plugin goal documentation](http://webdriverextensions.github.io/webdriverextensions-maven-plugin/install-drivers-mojo.html#drivers).

### Installing a Driver from an URL
If the driver is not available amongst the
[available drivers list](#available-drivers) you can install a driver by
providing an URL to the download location together with a checksum (to retrieve
the checksum run the plugin without providing a checksum once, the plugin will
then calculate and print the checksum for you).

E.g. to install the PhanthomJS driver
```xml
<driver>
    <name>phanthomjs</name>
    <platform>mac</platform>
    <bit>32</bit>
    <version>1.9.7</version>
    <url>http://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.7-macosx.zip</url>
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
    <version>1.1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>install-drivers</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <installationDirectory>/Users/andidev/drivers</installationDirectory>
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
    <version>1.1.0</version>
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



### Further Configurations
For more details on how to further configure this plugin please see the
[plugin goal documentation](http://webdriverextensions.github.io/webdriverextensions-maven-plugin/install-drivers-mojo.html).



## Changelog

#### 1.1.0 (2015 April 2)
- FEATURE Added support for using proxy configured in settings.xml

#### 1.0.1 (2014 September 2)
- BUGFIX - Fixed platform not provided bug
- BUGFIX - Removed dependency to java 7

#### 1.0.0 (2014 June 6)
- Initial release!

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
