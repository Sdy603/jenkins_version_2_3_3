package io.jenkins.plugins.sample;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    public void testOnCompletedSendsEventsForSuccessFailureAndAborted() throws Exception {
        DxRunListener listener = new TestableDxRunListener(config, sender);

        Run<?, ?> successRun = mockRun(Result.SUCCESS);
        Run<?, ?> failureRun = mockRun(Result.FAILURE);
        Run<?, ?> abortedRun = mockRun(Result.ABORTED);

        listener.onCompleted(successRun, taskListener);
        listener.onCompleted(failureRun, taskListener);
        listener.onCompleted(abortedRun, taskListener);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> runCaptor = ArgumentCaptor.forClass(Object.class);

        verify(sender, times(3)).send(payloadCaptor.capture(), runCaptor.capture());
        assertEquals(Arrays.asList(successRun, failureRun, abortedRun), runCaptor.getAllValues());
        assertEquals(Arrays.asList("success", "failure", "cancelled"), extractStatuses(payloadCaptor.getAllValues()));
        assertEquals(Arrays.asList("jenkins", "jenkins", "jenkins"), extractPipelineSources(payloadCaptor.getAllValues()));
    }

    private TaskListener createTaskListener() {
        TaskListener listener = mock(TaskListener.class);
        doReturn(new PrintStream(new ByteArrayOutputStream())).when(listener).getLogger();
        return listener;
    }

    private DxGlobalConfiguration mockConfiguredConfig() {
        DxGlobalConfiguration config = mock(DxGlobalConfiguration.class);
        when(config.isConfigured()).thenReturn(true);
        when(config.getRepositoryDenylist()).thenReturn("");
        return config;
    }

    private Run<?, ?> mockRun(Result result) throws Exception {
        @SuppressWarnings("unchecked")
        Run<?, ?> run = mock(Run.class);
        Job job = mock(Job.class);

        doReturn(result).when(run).getResult();
        doReturn(job).when(run).getParent();
        doReturn(42).when(run).getNumber();
        doReturn(1000L).when(run).getStartTimeInMillis();
        doReturn(500L).when(run).getDuration();
        doReturn(new EnvVars()).when(run).getEnvironment(any(TaskListener.class));
        doReturn("example/job").when(job).getFullName();

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
