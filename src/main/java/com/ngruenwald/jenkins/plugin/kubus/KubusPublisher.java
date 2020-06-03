package com.ngruenwald.jenkins.plugin.kubus;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Map;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public final class KubusPublisher extends Recorder{

    public static final String VERSION_FIXED_NAME = "fixedVersion";
    public static final String VERSION_FIXED_DISP = "Fixed Version String";
    public static final String VERSION_FILEPATTERN_NAME = "filePattern";
    public static final String VERSION_FILEPATTERN_DISP = "File Name Pattern";
    public static final String VERSION_BRANCHPATTERN_NAME = "branchPattern";
    public static final String VERSION_BRANCHPATTERN_DISP = "Branch Name Pattern";

    private String serverName;
    private String sourceFile;
    private String scmBranch;
    private String scmCommit;
    private String scmURL;
    private String component;
    private String versionType;
    private String versionValue;
    private String versionReplace;
    private String repository;
    private String platform;
    private String type;

    @DataBoundConstructor
    public KubusPublisher(
        String serverName,
        String sourceFile,
        String component,
        String versionType,
        String versionValue,
        String versionReplace,
        String scmBranch,
        String scmCommit,
        String scmURL,
        String repository,
        String platform,
        String type)
    {
        this.serverName = serverName;
        this.sourceFile = sourceFile;
        this.component = component;
        this.versionType = versionType;
        this.versionValue = versionValue;
        this.versionReplace = versionReplace;
        this.scmBranch = scmBranch;
        this.scmCommit = scmCommit;
        this.scmURL = scmURL;
        this.repository = repository;
        this.platform = platform;
        this.type = type;
    }

    @Override
    public boolean perform(
        AbstractBuild<?, ?> build,
        Launcher launcher,
        BuildListener listener) throws InterruptedException, IOException
    {
        Result buildResult = build.getResult();
        if (buildResult == null) {
            listener.getLogger().println("Kubus Publisher: Skipping because of missing build result");
            return true;
        }

        if (buildResult.isWorseOrEqualTo(Result.FAILURE)) {
            listener.getLogger().println(Messages.KubusPublisher_SkipFailure());
            return true;
        }

        KubusServer server = getServer(listener);
        if (server == null) {
            listener.error("Kubus server not found");
            return false;
        }

        String serverURL = server.getUrl();
        int apiVersion = server.getApiVersion();

        FilePath filePath = getSourcePath(build, listener);
        if (filePath == null) {
            return false;
        }

        String fileName = filePath.getName();
        String apiKey = getApiKey(server.getApiKeyId());
        Map<String, String> envVars = build.getEnvironment(listener);

        listener.getLogger().println(
            String.format("%s, %s, %s", versionType, versionValue, versionReplace)
        );

        String version = null;
        if (versionType.equals(VERSION_FIXED_NAME)) {
            version = Util.replaceMacro(versionValue, envVars);
        } else {
            String search = Util.replaceMacro(versionValue, envVars);
            String replace = Util.replaceMacro(versionReplace, envVars);
            String source = "";
            if (versionType.equals(VERSION_FILEPATTERN_NAME)) {
                source = fileName;
            }
            if (versionType.equals(VERSION_BRANCHPATTERN_NAME)) {
                source = Util.replaceMacro(scmBranch, envVars);
            }
            version = source.replaceAll(search, replace);
        }

        if (version == null) {
            listener.error("Could not determine artifact version");
            return false;
        }

        long   buildNumber = 0;
        String buildNumberStr = envVars.get("BUILD_NUMBER");
        String buildName = envVars.get("JOB_NAME");
        String buildNode = envVars.get("NODE_NAME");
        String buildURL  = envVars.get("BUILD_URL");

        if (buildNumberStr != null) {
            buildNumber = Long.parseLong(buildNumberStr);
        }
        if (buildName == null) {
            buildName = "";
        }
        if (buildNode == null) {
            buildNode = "";
        }
        if (buildURL == null) {
            buildURL = "";
        }

        JSONObject scmInfo = new JSONObject();
        scmInfo.put("branch", Util.replaceMacro(scmBranch, envVars));
        scmInfo.put("commit", Util.replaceMacro(scmCommit, envVars));
        scmInfo.put("url",    Util.replaceMacro(scmURL, envVars));

        JSONObject buildInfo = new JSONObject();
        buildInfo.put("number", buildNumber);
        buildInfo.put("name", buildName);
        buildInfo.put("node", buildNode);
        buildInfo.put("url", buildURL);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("component", Util.replaceMacro(component, envVars));
        jsonObject.put("version", Util.replaceMacro(version, envVars));
        jsonObject.put("scm", scmInfo);
        jsonObject.put("build", buildInfo);
        jsonObject.put("repository", Util.replaceMacro(repository, envVars));
        jsonObject.put("platform", Util.replaceMacro(platform, envVars));
        jsonObject.put("filename", Util.replaceMacro(fileName, envVars));
        jsonObject.put("type", type);

        filePath.act(new KubusUploader(
            serverURL, apiVersion, apiKey, jsonObject.toString(), filePath, listener
        ));

        build.addAction(
            new KubusAction(
                server.getName(),
                component,
                version,
                scmBranch,
                scmCommit,
                scmURL,
                buildNumber,
                buildName,
                buildNode,
                buildURL,
                repository,
                platform,
                fileName,
                type
            )
        );

        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getComponent() {
        return component;
    }

    public String getVersionType() {
        return versionType;
    }

    public String getVersionValue() {
        return versionValue;
    }

    public String getVersionReplace() {
        return versionReplace;
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

    public String getRepository() {
        return repository;
    }

    public String getPlatform() {
        return platform;
    }

    public String getType() {
        return type;
    }

    private KubusServer getServer(BuildListener listener) {
        KubusConfiguration descriptor =
            (KubusConfiguration) Jenkins.get().getDescriptor(KubusConfiguration.class);
        if (descriptor == null) {
            return null;
        }
        for (KubusServer server : descriptor.getServers()) {
            if (serverName.equals(server.getName())) {
                return server;
            }
        }
        return null;
    }

    private String getApiKey(String apiKeyId) {
        KubusApiKey c =
            CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                    KubusApiKey.class,
                    ACL.SYSTEM
                ),
                CredentialsMatchers.withId(apiKeyId)
            );
        return c != null ? c.getApiKey().getPlainText() : "";
    }

    private FilePath getSourcePath(
        AbstractBuild<?, ?> build,
        BuildListener listener) throws InterruptedException, IOException {

        FilePath workSpace = build.getWorkspace();
        if (workSpace == null) {
            listener.error("workspace not set");
            return null;
        }

        Map<String, String> envVars = build.getEnvironment(listener);

        // Find first matching file
        String[] patterns = sourceFile.split(",");
        for (String pattern : patterns) {
            String expanded = Util.replaceMacro(pattern.trim(), envVars);
            FilePath[] sourceFiles = null;

            FilePath tmp = new FilePath(workSpace, expanded);

            if (tmp.exists() && tmp.isDirectory()) {
                sourceFiles = tmp.list("**/*");
            } else {
                sourceFiles = workSpace.list(expanded);
            }

            if (sourceFiles.length > 0) {
                return sourceFiles[0];
            }
        }

        listener.error("Source file not found");
        return null;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(KubusPublisher.class);
            load();
        }

        protected DescriptorImpl(Class<? extends Publisher> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.KubusPublisher_DisplayName();
        }

        public FormValidation doCheckServerName(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.server_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckSourceFile(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.sourceFile_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckComponent(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.component_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckVersionType(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.versionType_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckVersionValue(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.versionValue_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckVersionReplace(@QueryParameter String value) {
            // TODO: only evaluate when pattern is used
            return FormValidation.ok();
        }

        public FormValidation doCheckScmBranch(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.scmBranch_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckScmCommit(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.scmCommit_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckScmURL(@QueryParameter String value) {
            return FormValidation.ok();
        }

        public FormValidation doCheckRepository(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.repository_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckPlatform(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.platform_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckType(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error(Messages.type_required());
            } else {
                return FormValidation.ok();
            }
        }

        public ListBoxModel doFillServerNameItems() {
            ListBoxModel model = new ListBoxModel();
            KubusConfiguration descriptor =
                (KubusConfiguration) Jenkins.get().getDescriptor(KubusConfiguration.class);
            if (descriptor != null) {
                for (KubusServer server : descriptor.getServers()) {
                    model.add(server.getName(), server.getName());
                }
            }
            return model;
        }

        public ListBoxModel doFillVersionTypeItems() {
            ListBoxModel model = new ListBoxModel();
            model.add(VERSION_FIXED_DISP, VERSION_FIXED_NAME);
            model.add(VERSION_FILEPATTERN_DISP, VERSION_FILEPATTERN_NAME);
            model.add(VERSION_BRANCHPATTERN_DISP, VERSION_BRANCHPATTERN_NAME);
            return model;
        }

        public ListBoxModel doFillTypeItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("Package", "package");
            model.add("Symbols", "symbols");
            return model;
        }
    }
}
