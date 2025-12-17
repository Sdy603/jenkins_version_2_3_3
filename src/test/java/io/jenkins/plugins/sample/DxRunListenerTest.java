package io.jenkins.plugins.sample;

import static org.junit.Assert.*;

import hudson.model.Result;
import org.junit.Test;

/** Basic tests for DxRunListener. */
public class DxRunListenerTest {

    @Test
    public void testResultMapping() {
        assertEquals("success", DxRunListener.mapResult(Result.SUCCESS));
        assertEquals("failure", DxRunListener.mapResult(Result.FAILURE));
        assertEquals("cancelled", DxRunListener.mapResult(Result.ABORTED));
        assertEquals("failure", DxRunListener.mapResult(Result.UNSTABLE));
        assertEquals("unknown", DxRunListener.mapResult(null));
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
}
