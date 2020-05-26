package com.ngruenwald.jenkins.plugin.kubus;

import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;


@Extension
public class KubusConfiguration extends GlobalConfiguration {

    private List<KubusServer> servers = new ArrayList<>();
    private transient Map<String, KubusServer> serverMap = new HashMap<>();

    public KubusConfiguration() {
        load();
        refreshServerMap();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        servers = req.bindJSONToList(KubusServer.class, json.get("servers"));
        refreshServerMap();
        save();
        return super.configure(req, json);
    }

    public List<KubusServer> getServers() {
        return servers;
    }

    public void addServer(KubusServer server) {
        servers.add(server);
        serverMap.put(server.getName(), server);
    }

    public void setServers(List<KubusServer> newServers) {
        servers = new ArrayList<>();
        serverMap = new HashMap<>();
        for (KubusServer server : newServers) {
            addServer(server);
        }
    }

    private boolean isEmptyOrNull(String value) {
        if (value == null) {
            return true;
        }
        if (value.isEmpty()) {
            return true;
        }
        return false;
    }

    public FormValidation doCheckName(@QueryParameter String id, @QueryParameter String value) {
        if (isEmptyOrNull(value)) {
            return FormValidation.error(Messages.name_required());
        } else if (serverMap.containsKey(value) && !serverMap.get(value).toString().equals(id)) {
            return FormValidation.error(Messages.name_exists(value));
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckUrl(@QueryParameter String value) {
        if (isEmptyOrNull(value)) {
            return FormValidation.error(Messages.url_required());
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckApiVersion(@QueryParameter Integer value) {
        if (value == null) {
            return FormValidation.error(Messages.apiVersion_required());
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckApiKeyId(@QueryParameter String value) {
        if (isEmptyOrNull(value)) {
            return FormValidation.error(Messages.apiKey_required());
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckConnectionTimeout(@QueryParameter String value) {
        if (value == null) {
            return FormValidation.error(Messages.connectionTimeout_required());
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckReadTimeout(@QueryParameter String value) {
        if (value == null) {
            return FormValidation.error(Messages.readTimeout_required());
        } else {
            return FormValidation.ok();
        }
    }

    @RequirePOST
    @Restricted(DoNotUse.class) // WebOnly
    public FormValidation doTestConnection(
        @QueryParameter String url,
        @QueryParameter int apiVersion,
        @QueryParameter String apiKeyId,
        @QueryParameter boolean ignoreCertificateErrors,
        @QueryParameter int connectionTimeout,
        @QueryParameter int readTimeout) {

        return FormValidation.error("not implemented");
    }

    public ListBoxModel doFillApiKeyIdItems(@QueryParameter String name, @QueryParameter String url) {
        if (Jenkins.get().hasPermission(Item.CONFIGURE)) {
            AbstractIdCredentialsListBoxModel<StandardListBoxModel, StandardCredentials> options =
                new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                        ACL.SYSTEM,
                        Jenkins.get(),
                        StandardCredentials.class,
                        URIRequirementBuilder.fromUri(url).build(),
                        new KubusCredentialMatcher()
                    );
            if (name != null && serverMap.containsKey(name)) {
                String apiKeyId = serverMap.get(name).getApiKeyId();
                options.includeCurrentValue(apiKeyId);
                for (ListBoxModel.Option option : options) {
                    if (option.value.equals(apiKeyId)) {
                        option.selected = true;
                    }
                }
            }
            return options;
        }
        return new StandardListBoxModel();
    }

    private void refreshServerMap() {
        serverMap.clear();
        for (KubusServer server : servers) {
            serverMap.put(server.getName(), server);
        }
    }
}
