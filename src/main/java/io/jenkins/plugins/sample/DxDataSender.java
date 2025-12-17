package io.jenkins.plugins.sample;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

/** Simple HTTP client for sending data to DX. */
public class DxDataSender {

    private static final Logger LOGGER = Logger.getLogger(DxDataSender.class.getName());

    private final DxGlobalConfiguration config;
    private final TaskListener listener;

    public DxDataSender(DxGlobalConfiguration config, TaskListener listener) {
        this.config = config;
        this.listener = listener;
    }

    public void send(String payload, Object build) {
        String dxBaseUrl = config.getDxBaseUrl();
        if (dxBaseUrl == null || dxBaseUrl.isBlank()) {
            listener.getLogger().println("DX: API base path not configured. Skipping.");
            return;
        }

        String fullUrl = dxBaseUrl + "/api/pipelineRuns.sync";

        if (!(build instanceof Run)) {
            listener.getLogger().println("DX: build context is required for credentials. Skipping.");
            return;
        }

        Run<?, ?> run = (Run<?, ?>) build;

        StringCredentials credentials = CredentialsProvider.findCredentialById(
                "dx-api-token", StringCredentials.class, run, Collections.emptyList());

        if (credentials == null) {
            listener.getLogger().println("DX: credentials not found for ID: dx-api-token");
            return;
        }

        String dxToken = credentials.getSecret().getPlainText();

        listener.getLogger().println("DX Payload: " + payload);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Authorization", "Bearer " + dxToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                listener.getLogger().println("DX: payload sent successfully. Response code: " + code);
            } else {
                listener.getLogger().println("DX: failed to send payload. Response code: " + code);
            }
        } catch (Exception e) {
            String msg = "DX: error sending data - " + e.getMessage();
            listener.getLogger().println(msg);
            LOGGER.log(Level.WARNING, msg, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
