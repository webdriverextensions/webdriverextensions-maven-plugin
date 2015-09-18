package com.github.webdriverextensions;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;

import static com.github.webdriverextensions.TestUtils.*;
import static com.github.webdriverextensions.Utils.isLinux;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractInstallDriverMojoTest extends AbstractMojoTestCase {
    private File installationDirectory;

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

    public static void logTestName(InstallDriversMojo mojo) {
        mojo.getLog().info("");
        mojo.getLog().info("");
        mojo.getLog().info("## TEST: " + new Exception().getStackTrace()[2].getMethodName());
    }
}
