package com.ngruenwald.jenkins.plugin.kubus;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;

@NameWith(KubusApiKey.NameProvider.class)
public interface KubusApiKey extends StandardCredentials {

    Secret getApiKey();

    class NameProvider extends CredentialsNameProvider<KubusApiKey> {
        @Override
        public String getName(KubusApiKey c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return Messages.KubusApiKey_name() + (description != null ? " (" + description + ")" : "");
        }
    }
}