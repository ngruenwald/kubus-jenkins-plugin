package com.ngruenwald.jenkins.plugin.kubus;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public final class KubusApiKeyImpl extends BaseStandardCredentials implements KubusApiKey {

    private Secret apiKey;

    @DataBoundConstructor
    public KubusApiKeyImpl(CredentialsScope scope, String id, String description, Secret apiKey) {
        super(scope, id, description);
        this.apiKey = apiKey;
    }

    @Override
    public Secret getApiKey() {
        return apiKey;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.KubusApiKey_name();
        }
    }
}
