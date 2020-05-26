package com.ngruenwald.jenkins.plugin.kubus;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import jenkins.model.Jenkins;

public class KubusServer extends AbstractDescribableImpl<KubusServer> {

    private final String name;
    private final String url;
    private final Integer apiVersion;
    private final String apiKeyId;
    private final boolean ignoreCertificateErrors;
    private final Integer connectionTimeout;
    private final Integer readTimeout;

    @DataBoundConstructor
    public KubusServer(
        String name,
        String url,
        Integer apiVersion,
        String apiKeyId,
        boolean ignoreCertificateErrors,
        Integer connectionTimeout,
        Integer readTimeout) {

        this.name = name;
        this.url = url;
        this.apiVersion = apiVersion;
        this.apiKeyId = apiKeyId;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Integer getApiVersion() {
        return apiVersion;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public boolean isIgnoreCertificateErrors() {
        return ignoreCertificateErrors;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    //@Restricted(NoExternalUse.class)
    public String getApiKey(String apiKeyId, Item item) {
        ItemGroup context = null != item ? item.getParent() : Jenkins.get();
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
            lookupCredentials(
                StandardCredentials.class,
                context,
                ACL.SYSTEM,
                URIRequirementBuilder.fromUri(url).build()),
            CredentialsMatchers.withId(apiKeyId));
        if (credentials != null) {
            if (credentials instanceof KubusApiKey) {
                return ((KubusApiKey) credentials).getApiKey().getPlainText();
            }
            if (credentials instanceof StringCredentials) {
                return ((StringCredentials) credentials).getSecret().getPlainText();
            }
        }
        throw new IllegalStateException("No credentials found for credentialsId: " + apiKeyId);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KubusServer> {
        public String getDisplayName() { return ""; }
    }
}