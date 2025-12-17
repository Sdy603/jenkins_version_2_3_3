package io.jenkins.plugins.sample;

import hudson.Extension;
import hudson.util.FormValidation;
import java.util.regex.Pattern;
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
    private String includeRepoPattern = ".*";
    private String includeJobPattern = ".*";
    private String includeBranchPattern = ".*";
    private String pipelineSource = "Jenkins";

    public DxGlobalConfiguration() {
        load();
        if (includeRepoPattern == null || includeRepoPattern.isEmpty()) {
            includeRepoPattern = ".*";
        }
        if (includeJobPattern == null || includeJobPattern.isEmpty()) {
            includeJobPattern = ".*";
        }
        if (includeBranchPattern == null || includeBranchPattern.isEmpty()) {
            includeBranchPattern = ".*";
        }
        if (pipelineSource == null || pipelineSource.isEmpty()) {
            pipelineSource = "Jenkins";
        }
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

    @Nullable
    public String getIncludeRepoPattern() {
        return includeRepoPattern;
    }

    @DataBoundSetter
    public void setIncludeRepoPattern(@Nullable String includeRepoPattern) {
        this.includeRepoPattern = includeRepoPattern;
        save();
    }

    @Nullable
    public String getIncludeJobPattern() {
        return includeJobPattern;
    }

    @DataBoundSetter
    public void setIncludeJobPattern(@Nullable String includeJobPattern) {
        this.includeJobPattern = includeJobPattern;
        save();
    }

    @Nullable
    public String getIncludeBranchPattern() {
        return includeBranchPattern;
    }

    @DataBoundSetter
    public void setIncludeBranchPattern(@Nullable String includeBranchPattern) {
        this.includeBranchPattern = includeBranchPattern;
        save();
    }

    @Nullable
    public String getPipelineSource() {
        return pipelineSource;
    }

    @DataBoundSetter
    public void setPipelineSource(@Nullable String pipelineSource) {
        this.pipelineSource = pipelineSource;
        save();
    }

    public boolean isConfigured() {
        return dxBaseUrl != null && !dxBaseUrl.trim().isEmpty();
    }

    public boolean shouldProcess(@Nullable String repo, @Nullable String jobName, @Nullable String branch) {
        if (!isConfigured()) {
            return false;
        }
        return matches(includeRepoPattern, repo)
                && matches(includeJobPattern, jobName)
                && matches(includeBranchPattern, branch);
    }

    private boolean matches(@Nullable String pattern, @Nullable String value) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        try {
            final Pattern compiled = Pattern.compile(pattern);
            return compiled.matcher(value).find();
        } catch (Exception e) {
            return true; // fail open if regex is invalid
        }
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

    public FormValidation doCheckIncludeRepoPattern(@QueryParameter String value) {
        return validateRegex(value, "Repository pattern");
    }

    public FormValidation doCheckIncludeJobPattern(@QueryParameter String value) {
        return validateRegex(value, "Job name pattern");
    }

    public FormValidation doCheckIncludeBranchPattern(@QueryParameter String value) {
        return validateRegex(value, "Branch pattern");
    }

    private FormValidation validateRegex(@Nullable String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return FormValidation.ok();
        }
        try {
            Pattern.compile(value);
            return FormValidation.ok();
        } catch (Exception e) {
            return FormValidation.error(fieldName + " has an invalid regex pattern.");
        }
    }
}
