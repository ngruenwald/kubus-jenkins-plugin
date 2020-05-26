package com.ngruenwald.jenkins.plugin.kubus;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class KubusAction implements RunAction2 {

    private transient Run run;

    private String serverName;
    private String component;
    private String version;
    private String scmBranch;
    private String scmCommit;
    private String scmURL;
    private long buildNumber;
    private String buildName;
    private String buildNode;
    private String buildURL;
    private String repository;
    private String platform;
    private String filePath;
    private String fileType;

    public KubusAction(
        String serverName,
        String component,
        String version,
        String scmBranch,
        String scmCommit,
        String scmURL,
        long buildNumber,
        String buildName,
        String buildNode,
        String buildURL,
        String repository,
        String platform,
        String filePath,
        String fileType) {

        this.serverName = serverName;
        this.component = component;
        this.version = version;
        this.scmBranch = scmBranch;
        this.scmCommit = scmCommit;
        this.scmURL = scmURL;
        this.buildNumber = buildNumber;
        this.buildName = buildName;
        this.buildNode = buildNode;
        this.buildURL = buildURL;
        this.repository = repository;
        this.platform = platform;
        this.filePath = filePath;
        this.fileType = fileType;
    }

    @Override
    public String getIconFileName() {
        return "package.png";
    }

    @Override
    public String getDisplayName() {
        return "Kubus";
    }

    @Override
    public String getUrlName() {
        return "kubus";
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run getRun() {
        return run;
    }

    public String getServerName() {
        return serverName;
    }

    public String getComponent() {
        return component;
    }

    public String getVersion() {
        return version;
    }

    public String getScmBranch() {
        return scmBranch;
    }

    public String getScmCommit() {
        return scmCommit;
    }

    public String getScmURL() {
        return scmURL;
    }

    public long getBuildNumber() {
        return buildNumber;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildNode() {
        return buildNode;
    }

    public String getBuildURL() {
        return buildURL;
    }

    public String getRepository() {
        return repository;
    }

    public String getPlatform() {
        return platform;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getType() {
        return fileType;
    }
}
