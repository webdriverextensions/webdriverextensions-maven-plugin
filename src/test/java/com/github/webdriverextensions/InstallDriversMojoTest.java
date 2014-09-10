package com.github.webdriverextensions;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;

public class InstallDriversMojoTest extends AbstractMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testNoConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-no-configuration-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");

        installDriversMojo.execute();
    }

    public void testConfiguration() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");

        installDriversMojo.execute();
    }

    public void testConfigurationInstallAllLatestDrivers() throws Exception {
        MavenProject project = getMavenProject("src/test/resources/test-mojo-configuration-install-all-latest-drivers-pom.xml");
        InstallDriversMojo installDriversMojo = (InstallDriversMojo) lookupConfiguredMojo(project, "install-drivers");
        installDriversMojo.repositoryUrl = Thread.currentThread().getContextClassLoader().getResource("repository.json");

        installDriversMojo.execute();
    }

    private MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }
}
