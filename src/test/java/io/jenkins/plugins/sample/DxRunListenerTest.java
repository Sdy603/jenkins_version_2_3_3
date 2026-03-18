package io.jenkins.plugins.sample;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** Basic tests for DxRunListener. */
public class DxRunListenerTest {

    private DxGlobalConfiguration config;
    private DxDataSender sender;
    private TaskListener taskListener;

    @Before
    public void setUp() {
        config = mockConfiguredConfig();
        sender = mock(DxDataSender.class);
        taskListener = createTaskListener();
    }

    @Test
    public void testResultMapping() {
        assertEquals("success", DxRunListener.mapResult(Result.SUCCESS));
        assertEquals("failure", DxRunListener.mapResult(Result.FAILURE));
        assertEquals("cancelled", DxRunListener.mapResult(Result.ABORTED));
        assertEquals("failure", DxRunListener.mapResult(Result.UNSTABLE));
        assertEquals("cancelled", DxRunListener.mapResult(Result.NOT_BUILT));
        assertEquals("failure", DxRunListener.mapResult(null));
    }

    @Test
    public void testRepositoryDenylistMatching() {
        assertTrue(DxRunListener.isRepositoryDenied("example-repo", "example-repo"));
        assertTrue(DxRunListener.isRepositoryDenied("example-repo", "another, example-repo"));
        assertTrue(DxRunListener.isRepositoryDenied("EXAMPLE-repo", "example-repo\nsecond"));
        assertFalse(DxRunListener.isRepositoryDenied("example-repo", "other"));
        assertFalse(DxRunListener.isRepositoryDenied("", "example-repo"));
        assertFalse(DxRunListener.isRepositoryDenied("example-repo", ""));
    }

    @Test
    public void testExtractWorkspaceRepositoryStripsBranchAndUsesLastTwoSegments() {
        assertEquals("workspace/repository", DxRunListener.extractWorkspaceRepository("workspace/repository/main", "main"));
        assertEquals(
                "workspace/repository",
                DxRunListener.extractWorkspaceRepository("org/workspace/repository/feature/JIRA-123", "feature/JIRA-123"));
        assertEquals(
                "ciex/dx-test-repo",
                DxRunListener.extractWorkspaceRepository(
                        "ciex/dx-test-repo/feature%2Fdx-jenkins-plugin-test", "feature/dx-jenkins-plugin-test"));
        assertEquals("workspace/repository", DxRunListener.extractWorkspaceRepository("workspace/repository", ""));
    }

    @Test
    public void testOnCompletedDecodesEscapedJobNamesInPayloadFields() throws Exception {
        DxRunListener listener = new TestableDxRunListener(config, sender);

        Run<?, ?> run = mockRun(Result.SUCCESS, taskListener, new EnvVars(), "ciex/dx-test-repo/feature%2Fdx-jenkins-plugin-test");

        listener.onCompleted(run, taskListener);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(1)).send(payloadCaptor.capture(), org.mockito.ArgumentMatchers.eq(run));

        JSONObject payload = new JSONObject(payloadCaptor.getValue());
        assertEquals("ciex/dx-test-repo", payload.optString("repository"));
        assertEquals("ciex/dx-test-repo/feature/dx-jenkins-plugin-test", payload.optString("pipeline_name"));
        assertEquals("ciex/dx-test-repo/feature/dx-jenkins-plugin-test", payload.optString("source_id"));
    }


    @Test
    public void testOnCompletedUsesWorkspaceRepositoryEvenWhenGitUrlExists() throws Exception {
        DxRunListener listener = new TestableDxRunListener(config, sender);

        EnvVars envVars = new EnvVars();
        envVars.put("GIT_URL", "https://bitbucket.org/workspace/other-repo.git");
        Run<?, ?> run = mockRun(Result.SUCCESS, taskListener, envVars);

        listener.onCompleted(run, taskListener);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(1)).send(payloadCaptor.capture(), org.mockito.ArgumentMatchers.eq(run));
        assertEquals("example/job", new JSONObject(payloadCaptor.getValue()).optString("repository"));
    }

    @Test
    public void testOnCompletedSendsEventsForSuccessFailureAndAborted() throws Exception {
        DxRunListener listener = new TestableDxRunListener(config, sender);

        Run<?, ?> successRun = mockRun(Result.SUCCESS, taskListener);
        Run<?, ?> failureRun = mockRun(Result.FAILURE, taskListener);
        Run<?, ?> abortedRun = mockRun(Result.ABORTED, taskListener);

        listener.onCompleted(successRun, taskListener);
        listener.onCompleted(failureRun, taskListener);
        listener.onCompleted(abortedRun, taskListener);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> runCaptor = ArgumentCaptor.forClass(Object.class);

        verify(sender, times(3)).send(payloadCaptor.capture(), runCaptor.capture());
        assertEquals(Arrays.asList(successRun, failureRun, abortedRun), runCaptor.getAllValues());
        assertEquals(Arrays.asList("success", "failure", "cancelled"), extractStatuses(payloadCaptor.getAllValues()));
        assertEquals(Arrays.asList("jenkins", "jenkins", "jenkins"), extractPipelineSources(payloadCaptor.getAllValues()));
        assertEquals(
                Arrays.asList("example/job", "example/job", "example/job"),
                extractRepositories(payloadCaptor.getAllValues()));
    }

    private TaskListener createTaskListener() {
        TaskListener listener = mock(TaskListener.class);
        doReturn(new PrintStream(new ByteArrayOutputStream())).when(listener).getLogger();
        return listener;
    }

    private DxGlobalConfiguration mockConfiguredConfig() {
        DxGlobalConfiguration config = mock(DxGlobalConfiguration.class);
        doReturn(true).when(config).isConfigured();
        doReturn("").when(config).getRepositoryDenylist();
        return config;
    }

    private Run<?, ?> mockRun(Result result, TaskListener listener) throws Exception {
        return mockRun(result, listener, new EnvVars());
    }

    private Run<?, ?> mockRun(Result result, TaskListener listener, EnvVars envVars) throws Exception {
        return mockRun(result, listener, envVars, "example/job");
    }

    private Run<?, ?> mockRun(Result result, TaskListener listener, EnvVars envVars, String fullJobName) throws Exception {
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);

        doReturn(result).when(run).getResult();
        doReturn(job).when(run).getParent();
        doReturn(42).when(run).getNumber();
        doReturn(1000L).when(run).getStartTimeInMillis();
        doReturn(500L).when(run).getDuration();
        doReturn(envVars).when(run).getEnvironment(listener);
        doReturn(fullJobName).when(job).getFullName();

        return run;
    }

    private static List<String> extractStatuses(List<String> payloads) {
        List<String> statuses = new ArrayList<>();
        for (String payload : payloads) {
            statuses.add(new JSONObject(payload).optString("status"));
        }
        return statuses;
    }

    private static List<String> extractPipelineSources(List<String> payloads) {
        List<String> pipelineSources = new ArrayList<>();
        for (String payload : payloads) {
            pipelineSources.add(new JSONObject(payload).optString("pipeline_source"));
        }
        return pipelineSources;
    }

    private static List<String> extractRepositories(List<String> payloads) {
        List<String> repositories = new ArrayList<>();
        for (String payload : payloads) {
            repositories.add(new JSONObject(payload).optString("repository"));
        }
        return repositories;
    }

    private static class TestableDxRunListener extends DxRunListener {
        private final DxGlobalConfiguration configuration;
        private final DxDataSender sender;

        TestableDxRunListener(DxGlobalConfiguration configuration, DxDataSender sender) {
            this.configuration = configuration;
            this.sender = sender;
        }

        @Override
        DxGlobalConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        DxDataSender createDxDataSender(DxGlobalConfiguration config, TaskListener listener) {
            return sender;
        }
    }
}
