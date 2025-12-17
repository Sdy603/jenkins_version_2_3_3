package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.util.FormValidation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class DxGlobalConfiguration extends GlobalConfiguration {

    private String dxBaseUrl;

    public DxGlobalConfiguration() {
        load();
    }

    public static DxGlobalConfiguration get() {
        return GlobalConfiguration.all().get(DxGlobalConfiguration.class);
    }

    @Nullable
    public String getDxBaseUrl() {
        return dxBaseUrl;
    }

    @DataBoundSetter
    public void setDxBaseUrl(@Nullable String dxBaseUrl) {
        this.dxBaseUrl = dxBaseUrl;
        save();
    }

    public boolean isConfigured() {
        return dxBaseUrl != null && !dxBaseUrl.trim().isEmpty();
    }

    @Override
    public boolean configure(@Nonnull StaplerRequest req, @Nonnull JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public FormValidation doCheckDxBaseUrl(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.warning("DX API base URL is empty.");
        }
        if (!value.startsWith("https://")) {
            return FormValidation.warning("DX base URL should start with https://");
        }
        return FormValidation.ok();
    }

}
