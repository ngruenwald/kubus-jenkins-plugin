package com.ngruenwald.jenkins.plugin.kubus;

import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;

public class KubusPublisherTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final String serverName = "triangle";
    private final String sourceFile = "build/release/*.rpm";
    private final String component = "foo";
    private final String versionType = "fixedString";
    private final String versionValue = "0.0.1";
    private final String versionReplace = "";
    private final String scmBranch = "master";
    private final String scmCommit = "deadbeef2020";
    private final String scmURL = "http://bar.org/foo.git";
    private final String repository = "devel";
    private final String platform = "noarch";
    private final String type = "package";

    public void testConfigRoundtrip() throws Exception
    {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(
            new KubusPublisher(
                serverName, sourceFile,
                component, versionType,
                versionValue, versionReplace,
                scmBranch, scmCommit, scmURL,
                repository, platform, type
            )
        );

        project = jenkins.configRoundtrip(project);

        KubusPublisher kubusPub =
            new KubusPublisher(
                serverName, sourceFile,
                component, versionType,
                versionValue, versionReplace,
                scmBranch, scmCommit, scmURL,
                repository, platform, type
            );
        jenkins.assertEqualDataBoundBeans(kubusPub, project.getPublishersList().get(0));
    }
}
