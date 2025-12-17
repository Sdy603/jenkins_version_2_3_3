package io.jenkins.plugins.sample;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.model.listeners.RunListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.MailAddressResolver;
import javax.annotation.Nonnull;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.metadata.ContributorMetadataAction;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import org.json.JSONObject;

/** Listener that publishes pipeline run metadata to the DX API. */
@Extension
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
public class DxRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onCompleted(Run<?, ?> run, @Nonnull TaskListener listener) {
        Result result = run.getResult();
        if (result == null || !result.equals(Result.SUCCESS)) {
            return;
        }

        DxGlobalConfiguration config = DxGlobalConfiguration.get();
        if (config == null || !config.isConfigured()) {
            listener.getLogger().println("DX: plugin not configured. Skipping.");
            return;
        }

        String repoUrl = "";
        String commitSha = "";
        String branchName = "";
        String targetBranch = "";

        SCMRevisionAction scmRevisionAction = run.getAction(SCMRevisionAction.class);
        String prNumber = "";
        if (scmRevisionAction != null && scmRevisionAction.getRevision() != null) {
            SCMHead head = scmRevisionAction.getRevision().getHead();
            if (head instanceof ChangeRequestSCMHead) {
                ChangeRequestSCMHead changeRequestHead = (ChangeRequestSCMHead) head;
                branchName = changeRequestHead.getName();
                targetBranch = changeRequestHead.getTarget().getName();
                prNumber = changeRequestHead.getId();
            } else if (branchName == null || branchName.isEmpty()) {
                branchName = head.getName();
            }

            if (commitSha == null || commitSha.isEmpty()) {
                commitSha = scmRevisionAction.getRevision().toString();
            }
        }

        if (branchName != null && !branchName.isEmpty()) {
            branchName = branchName
                    .replaceFirst("^refs/heads/", "")
                    .replaceFirst("^refs/remotes/origin/", "")
                    .replaceFirst("^origin/", "");
        }
        if (targetBranch != null && !targetBranch.isEmpty()) {
            targetBranch = targetBranch
                    .replaceFirst("^refs/heads/", "")
                    .replaceFirst("^refs/remotes/origin/", "")
                    .replaceFirst("^origin/", "");
        }

        String userEmail = "";
        ContributorMetadataAction contributor = run.getAction(ContributorMetadataAction.class);
        if (contributor != null && contributor.getContributorEmail() != null) {
            userEmail = contributor.getContributorEmail();
        }

        if (userEmail.isEmpty() && run instanceof AbstractBuild) {
            @SuppressWarnings("unchecked")
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
            for (ChangeLogSet<? extends ChangeLogSet.Entry> cs : build.getChangeSets()) {
                for (ChangeLogSet.Entry entry : cs) {
                    User author = entry.getAuthor();
                    if (author != null) {
                        String email = MailAddressResolver.resolve(author);
                        if (email != null && !email.isEmpty()) {
                            userEmail = email;
                            break;
                        }
                    }
                }
                if (!userEmail.isEmpty()) {
                    break;
                }
            }
        }

        if (userEmail.isEmpty()) {
            hudson.model.Cause.UserIdCause userIdCause = run.getCause(hudson.model.Cause.UserIdCause.class);
            if (userIdCause != null) {
                String userId = userIdCause.getUserId();
                if (userId != null) {
                    User buildUser = User.getById(userId, false);
                    if (buildUser != null) {
                        String fallbackEmail = MailAddressResolver.resolve(buildUser);
                        if (fallbackEmail != null && !fallbackEmail.isEmpty()) {
                            userEmail = fallbackEmail;
                            listener.getLogger().println("DX: fallback email found from build user.");
                        }
                    }
                }
            }
        }

        String jobName = run.getParent().getFullName();

        long start = run.getStartTimeInMillis() / 1000;
        long finish = (run.getStartTimeInMillis() + run.getDuration()) / 1000;
        String status = mapResult(result);
        if (status == null || status.isEmpty()) {
            status = "unknown";
        }

        String repositoryName = extractRepositoryName(repoUrl);

        String pipelineName = jobName;
        if (pipelineName == null || pipelineName.isEmpty()) {
            pipelineName = "jenkins-" + jobName;
        }
        String referenceId = jobName + " #" + run.getNumber();
        String sourceId = jobName;

        // Get the pipeline source based on hostname mapping
        String pipelineSource = HostnameMappingService.getPipelineSourceForCurrentHost();
        listener.getLogger().println("DX: Using pipeline source: " + pipelineSource);

        // Log additional debugging info
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            listener.getLogger().println("DX: Detected hostname: " + hostname);
        } catch (Exception e) {
            listener.getLogger().println("DX: Could not determine hostname: " + e.getMessage());
        }

        JSONObject payload = new JSONObject();
        payload.put("pipeline_name", pipelineName);
        payload.put("pipeline_source", HostnameMappingService.getPipelineSourceForCurrentHost());
        payload.put("reference_id", referenceId);
        payload.put("source_id", sourceId);
        payload.put("started_at", start);
        payload.put("finished_at", finish);
        payload.put("status", status);
        payload.put("repository", repositoryName);
        payload.put("source_url", repoUrl);
        payload.put("head_branch", branchName);
        if (targetBranch != null && !targetBranch.isEmpty()) {
            payload.put("base_branch", targetBranch);
        }
        payload.put("commit_sha", commitSha != null ? commitSha : "");
        if (prNumber != null && !prNumber.isEmpty()) {
            payload.put("pr_number", prNumber);
        }
        payload.put("email", userEmail);

        System.out.println("DX Payload:");
        System.out.println(payload.toString(2));

        DxDataSender dxSender = new DxDataSender(config, listener);
        dxSender.send(payload.toString(), run);
    }

    static String mapResult(Result result) {
        if (result == null) {
            return "unknown";
        }
        if (result.equals(Result.SUCCESS)) {
            return "success";
        } else if (result.equals(Result.FAILURE)) {
            return "failure";
        } else if (result.equals(Result.ABORTED)) {
            return "cancelled";
        } else if (result.equals(Result.UNSTABLE)) {
            return "failure";
        } else {
            return "unknown";
        }
    }

    private static String extractRepositoryName(String repoUrl) {
        if (repoUrl == null || repoUrl.isEmpty()) {
            return "";
        }
        String cleaned = repoUrl.replaceAll("\\.git$", "");
        String[] parts = cleaned.split("[/:]");
        return parts[parts.length - 1];
    }
}
