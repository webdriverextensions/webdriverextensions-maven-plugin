## Changelog WebDriver Extensions Maven Plugin

### Unreleased
#### ⚠ Breaking
#### ⭐ New Features
#### 🐞 Bugs Fixed

### 4.0.0
#### ⚠ Breaking
- Requires Maven 3.9.6

#### ⭐ New Features
- Verified to work with Java 21

#### 🐞 Bugs Fixed
- Updates vulnerable version of commons-compress (CVE-2023-42503, CVE-2024-26308)

### 3.4.0 (2022 December 18)
#### ⚠ Breaking
#### ⭐ New Features
- IMPROVEMENT Added support for different OS/CPU architectures such as AARCH64 (aka ARM64) [Issue 65](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/65)

#### 🐞 Bugs Fixed

### 3.3.0 (2021 December 13)
- BUGFIX `keepDownloadedWebdrivers` did not work as expected
- IMPROVEMENT configure download timeouts and retry attempts
- IMPROVEMENT Added support for Java 17 [Issue 56](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/56)
- BUGFIX failed to move non-empty directories of downloaded drivers on Windows [Issue 56](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/56)
- BUGFIX download directory was not always created [Issue 56](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/56)
- BUGFIX file extraction may fail for archives with directory entries without the D attribute [Issue 50](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/50). Thanks to [@ikucheryavenkov](https://github.com/ikucheryavenkov)
- IMPROVEMENT Added user properties for most of the settings
- IMPROVEMENT Support installing drivers when `skipTests` is true [Issue #49](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/49)
- IMPROVEMENT add option to automatically set Selenium system properties [Issue #26](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/26)

### 3.2.0 (2019 June 2)
- IMPROVEMENT Capability for custom file name with binaries [PR 42](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/42)
- IMPROVEMENT Maven offline mode should be honored [PR 39](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/39)
- IMPROVEMENT Use separate temp-folders [PR 37](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/37)
- IMPROVEMENT Add support for skipping if skipTests is defined [PR 35](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/35)

### 3.1.3 (2017 October 27)
- BUGFIX Made Opera driver executable (Thanks to [@svenruppert](https://github.com/svenruppert))

### 3.1.2 (2017 June 7)
- BUGFIX Fixed issue where latest driver where not downloaded when only name was provided for driver

### 3.1.1 (2016 October 18)
- MAJOR BUGFIX Fixed issue where updating driver was not possible

### 3.1.0 [DO NOT USE THIS RELEASE, USE 3.1.1 INSTEAD] (2016 October 18)
- BUGFIX Fixed [issue 24](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/24) Geckodriver on macOS shows as posix tar
- REFACTORING Cache folder name changed from cache to downloads

### 3.0.2 (2016 July 13)
- BUGFIX Fixed [issue 21](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/21) with failing on extracting chromedriver on windows machines

### 3.0.1 (2016 July 5)
- BUGFIX Fixed debug message on error not shown on windows machines

### 3.0.0 (2016 July 1)
- Added support for geckodriver (Marionette) (Thanks to [@pumano](https://github.com/pumano))
- Added support for edgedriver (Microsoft WebDriver)
- Added support for operadriver

### 2.2.0 (2016 June 13)
- Fixed bitDetection for internetexplorerdriver on Windows 10 enviroments 
- Fixed bitDetection for phantomjs (Thanks to [@lkwg82](https://github.com/lkwg82))

### 2.1.0 (2016 May 24)
- IMPROVEMENT No need to manually delete drivers when download fails [issue 11](https://github.com/webdriverextensions/webdriverextensions-maven-plugin/issues/11)
- IMPROVEMENT Improved logs to exactly show what the install goal is doing, e.g.
```
[info] chromedriver-mac-32bit version 2.20.0
[info]   Downloading "http://chromedriver.storage.googleapis.com/2.20/chromedriver_mac32.zip" to "/var/folders/mc/fd7bwdvd4c19wwhw0dmt2ngm0000gn/T/webdriverextensions-maven-plugin/temp/chromedriver_mac32.zip"
[info]   Extracting "/var/folders/mc/fd7bwdvd4c19wwhw0dmt2ngm0000gn/T/webdriverextensions-maven-plugin/temp/chromedriver_mac32.zip" to temp folder
[info]   Moving "/var/folders/mc/fd7bwdvd4c19wwhw0dmt2ngm0000gn/T/webdriverextensions-maven-plugin/temp/chromedriver_mac32/chromedriver" to "/Users/anders/Workspace/webdriverextensions-maven-plugin/src/test/resources/drivers/chromedriver-mac-32bit"
```
- IMPROVEMENT Print error message in downloaded file if present, e.g.
```
[info] phantomjs-linux-64bit version 1.9.8
[info]   Downloading "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-1.9.8-linux-x86_64.tar.bz2" to "/var/folders/mc/fd7bwdvd4c19wwhw0dmt2ngm0000gn/T/webdriverextensions-maven-plugin/temp/phantomjs-1.9.8-linux-x86_64.tar.bz2"
[info]   Downloaded driver file contains the following error message
[info]   <?xml version="1.0" encoding="UTF-8"?>
[info]   <Error><Code>AccessDenied</Code><Message>Request has expired</Message><Expires>2016-04-19T21:53:01Z</Expires><ServerTime>2016-04-19T21:57:53Z</ServerTime><RequestId>A7C783167E23A035</RequestId><HostId>QgT8M6Wsn6CdvOPcL1Iu3bpSmPYc9fLIhvSbGTrWRmQYmX6ZejV76VoeNeNnCYt6yGvb/0HBRdY=</HostId></Error>
```
- REFACTORING Lots of it to make the code more readable

### 2.0.0 (2015 September 20)
- FEATURE Added support for extracting bz2 files (Thanks to [@lkwg82](https://github.com/lkwg82))
- FEATURE Added support for only extracting specific files with the `fileMatchInside` driver configuration parameter (Thanks to [@lkwg82](https://github.com/lkwg82))
- FEATURE Added support to cache downloaded drivers with the `keepDownloadedWebdrivers` plugin configuration parameter (Thanks to [@lkwg82](https://github.com/lkwg82))
- PHANTOMJS Added official support for phantomjs (Thanks to [@lkwg82](https://github.com/lkwg82))
- JAVA 7 REQUIREMENT Now compiled with java 7

### 1.1.0 (2015 April 2)
- FEATURE Added support for using proxy configured in settings.xml

### 1.0.1 (2014 September 2)
- BUGFIX Fixed platform not provided bug
- BUGFIX Removed dependency to java 7

### 1.0.0 (2014 June 6)
- Initial release!
