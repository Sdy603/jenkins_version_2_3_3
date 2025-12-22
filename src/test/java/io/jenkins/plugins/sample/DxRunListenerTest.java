package io.jenkins.plugins.sample;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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
import org.junit.Test;

/** Basic tests for DxRunListener. */
public class DxRunListenerTest {

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
    public void testOnCompletedSendsEventsForSuccessFailureAndAborted() throws Exception {
        RecordingDxDataSender sender = new RecordingDxDataSender();
        DxGlobalConfiguration config = mockConfiguredConfig();
        DxRunListener listener = new TestableDxRunListener(config, sender);

        listener.onCompleted(mockRun(Result.SUCCESS), createTaskListener());
        listener.onCompleted(mockRun(Result.FAILURE), createTaskListener());
        listener.onCompleted(mockRun(Result.ABORTED), createTaskListener());

        assertEquals(3, sender.getSendCount());
        assertEquals(Arrays.asList("success", "failure", "cancelled"), sender.getStatuses());
        assertEquals(Arrays.asList("jenkins", "jenkins", "jenkins"), sender.getPipelineSources());
    }

    private TaskListener createTaskListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(new PrintStream(new ByteArrayOutputStream()));
        return listener;
    }

    private DxGlobalConfiguration mockConfiguredConfig() {
        DxGlobalConfiguration config = mock(DxGlobalConfiguration.class);
        when(config.isConfigured()).thenReturn(true);
        when(config.getDxBaseUrl()).thenReturn("https://dx.example.test");
        when(config.getRepositoryDenylist()).thenReturn("");
        return config;
    }

    private Run<?, ?> mockRun(Result result) throws Exception {
        @SuppressWarnings("unchecked")
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);

        when(run.getResult()).thenReturn(result);
        doReturn(job).when(run).getParent();
        when(run.getNumber()).thenReturn(42);
        when(run.getStartTimeInMillis()).thenReturn(1000L);
        when(run.getDuration()).thenReturn(500L);
        when(run.getEnvironment(any(TaskListener.class))).thenReturn(new EnvVars());
        when(run.getAction(any(Class.class))).thenReturn(null);
        when(run.getCause(any(Class.class))).thenReturn(null);
        when(job.getFullName()).thenReturn("example/job");

        return run;
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

    private static class RecordingDxDataSender extends DxDataSender {
        private final List<String> statuses = new ArrayList<>();
        private final List<String> pipelineSources = new ArrayList<>();
        private int sendCount = 0;

        RecordingDxDataSender() {
            super(null, null);
        }

        @Override
        public void send(String payload, Object build) {
            sendCount++;
            JSONObject jsonPayload = new JSONObject(payload);
            statuses.add(jsonPayload.optString("status"));
            pipelineSources.add(jsonPayload.optString("pipeline_source"));
        }

        int getSendCount() {
            return sendCount;
        }

        List<String> getStatuses() {
            return statuses;
        }

        List<String> getPipelineSources() {
            return pipelineSources;
        }
    }
}
