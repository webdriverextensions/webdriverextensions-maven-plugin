package com.github.webdriverextensions;

import java.io.File;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

public class InstallDriversMojoTest extends AbstractMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testNoConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-no-configuration-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("## TEST: testNoConfiguration");

        installDriversMojo.execute();
    }

    public void testConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("## TEST: testConfiguration");

        installDriversMojo.execute();
    }

    public void testConfigurationInstallAllLatestDrivers() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-install-all-latest-drivers-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("## TEST: testConfigurationInstallAllLatestDrivers");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        installDriversMojo.execute();
    }

    public void testRaiseErrorWhenDriverWasNotFoundInConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom_not_found_driver.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.getLog().info("## TEST: testRaiseErrorWhenDriverWasNotFoundInConfiguration");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        try {
            installDriversMojo.execute();
            fail("should raise an exception");
        } catch (MojoExecutionException e) {
            assertEquals("could not found driver: {\"name\":\"phantooomjs\",\"platform\":\"linux\",\"bit\":\"32\",\"version\":\"1.9.7\"}",e.getMessage());
        }
    }


    public void ignore_testConfigurationExtractPhantomJSDriverFromTarBz2() throws Exception {
        try {
            MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom_phantomjs-extract.xml");
            InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
            installDriversMojo.getLog().info("## TEST: testConfigurationExtractPhantomJSDriverFromTarBz2");
            installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

            installDriversMojo.execute();
        } finally {
            File dir = new File("src/test/resources/target_phantomjs-extract-test");
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
                dir.delete();
            }
        }
    }

    private MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }
}
