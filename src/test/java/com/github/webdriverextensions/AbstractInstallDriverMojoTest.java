package com.github.webdriverextensions;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.junit.Assert;

import java.io.File;

import static com.github.webdriverextensions.Utils.FAKED_BIT_PROPERTY_KEY;
import static com.github.webdriverextensions.Utils.FAKED_OS_NAME_PROPERTY_KEY;

public abstract class AbstractInstallDriverMojoTest extends AbstractMojoTestCase {

    public File installationDirectory;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (installationDirectory != null) {
            FileUtils.deleteDirectory(installationDirectory);
        }
    }

    public MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }

    public InstallDriversMojo getMojo(String pomPath, String goal) throws Exception {
        MavenProject project = getMavenProject(pomPath);
        InstallDriversMojo mojo = (InstallDriversMojo) lookupConfiguredMojo(project, goal);

        // some global test preparations
        installationDirectory = mojo.installationDirectory;
        logTestName(mojo);

        return mojo;
    }

    public void logTestName(InstallDriversMojo mojo) {
        mojo.getLog().info("");
        mojo.getLog().info("");
        mojo.getLog().info("## TEST: " + new Exception().getStackTrace()[2].getMethodName());
    }

    public void assertDriverIsInstalled(String driverFileName, File installationDirectory) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        for (File file : installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(driverFileName.replace(".exe", "") + ".version")) {
                foundDriverVersionFile = true;
            }
        }
        if (!foundDriverFile) {
            Assert.fail("Driver with file name " + driverFileName + " was not found in the installation directory");
        }
        if (!foundDriverVersionFile) {
            Assert.fail("Driver version file with file name " + driverFileName + ".version was not found in the installation directory");
        }
    }

    public void assertDriverIsNotInstalled(String driverFileName, File installationDirectory) {
        boolean foundDriverFile = false;
        boolean foundDriverVersionFile = false;
        for (File file : installationDirectory.listFiles()) {
            if (file.getName().equals(driverFileName)) {
                foundDriverFile = true;
            }
            if (file.getName().equals(driverFileName + ".version")) {
                foundDriverVersionFile = true;
            }
        }
        if (foundDriverFile) {
            Assert.fail("Driver with file name " + driverFileName + " was found in the installation directory when it should not have been");
        }
        if (foundDriverVersionFile) {
            Assert.fail("Driver version file with file name " + driverFileName + ".version was not found in the installation directory when it should not have been");
        }
    }

    public void fakePlatformToBeLinux() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "linux");
    }

    public void fakePlatformToBeWindows() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "windows");
    }

    public void fakePlatformToBeMac() {
        System.setProperty(FAKED_OS_NAME_PROPERTY_KEY, "mac");
    }

    public void fakeBitToBe64() {
        System.setProperty(FAKED_BIT_PROPERTY_KEY, "64");
    }

    public void fakeBitToBe32() {
        System.setProperty(FAKED_BIT_PROPERTY_KEY, "32");
    }
}
