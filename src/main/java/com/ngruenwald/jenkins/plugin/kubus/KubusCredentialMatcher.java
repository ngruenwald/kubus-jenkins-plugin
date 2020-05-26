package com.ngruenwald.jenkins.plugin.kubus;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;

public class KubusCredentialMatcher implements CredentialsMatcher {

    private static final long serialVersionUID = 3424297904062370211L;

    @Override
    public boolean matches(@NonNull Credentials credentials) {
        try {
            return credentials instanceof KubusApiKey || credentials instanceof StringCredentials;
        } catch (Throwable e) {
            return false;
        }
    }
}
