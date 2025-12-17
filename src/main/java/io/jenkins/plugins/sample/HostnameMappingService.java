package io.jenkins.plugins.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for mapping Jenkins hostnames to pipeline sources using the CSV configuration.
 */
public class HostnameMappingService {

    private static final Logger LOGGER = Logger.getLogger(HostnameMappingService.class.getName());
    private static final String CSV_RESOURCE_PATH = "/jenkins-server.csv";
    private static final String DEFAULT_PIPELINE_SOURCE = "jenkins-unmapped";

    private static volatile Map<String, String> hostnameMapping = null;
    private static volatile boolean initialized = false;

    /**
     * Gets the pipeline source for the current Jenkins hostname.
     * @return The mapped pipeline source or "jenkins-unmapped" if not found
     */
    public static String getPipelineSourceForCurrentHost() {
        if (!initialized) {
            initializeMapping();
        }

        try {
            String hostname = getCurrentHostname();
            LOGGER.fine("Looking up pipeline source for hostname: " + hostname);

            String pipelineSource = hostnameMapping.get(hostname);

            if (pipelineSource != null && !pipelineSource.trim().isEmpty()) {
                LOGGER.fine("Found pipeline source mapping for hostname '" + hostname + "': " + pipelineSource);
                return pipelineSource;
            } else {
                LOGGER.info(
                        "No mapping found for hostname '" + hostname + "', using default: " + DEFAULT_PIPELINE_SOURCE);
                return DEFAULT_PIPELINE_SOURCE;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting pipeline source for current hostname", e);
            return DEFAULT_PIPELINE_SOURCE;
        }
    }

    /**
     * Gets the current hostname of the Jenkins server.
     * @return The hostname string
     * @throws Exception if hostname cannot be determined
     */
    private static String getCurrentHostname() throws Exception {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not get local hostname, trying environment variable", e);
            String envHostname = System.getenv("HOSTNAME");
            if (envHostname != null && !envHostname.trim().isEmpty()) {
                return envHostname;
            }
            throw new Exception("Could not determine hostname");
        }
    }

    /**
     * Initializes the hostname mapping by reading the CSV file.
     */
    private static synchronized void initializeMapping() {
        if (initialized) {
            return;
        }

        Map<String, String> mapping = new HashMap<>();

        InputStream is = HostnameMappingService.class.getResourceAsStream(CSV_RESOURCE_PATH);
        if (is == null) {
            LOGGER.log(Level.SEVERE, "CSV resource not found at: " + CSV_RESOURCE_PATH);
            hostnameMapping = mapping;
            initialized = true;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                // Parse CSV line (assuming format: "hostname","description")
                String[] parts = parseCsvLine(line);
                if (parts.length >= 2) {
                    String hostname = parts[0].trim();
                    String description = parts[1].trim();

                    if (!hostname.isEmpty() && !description.isEmpty()) {
                        mapping.put(hostname, description);
                        LOGGER.fine("Added mapping: " + hostname + " -> " + description);
                    }
                }
            }

            LOGGER.info("Successfully loaded " + mapping.size() + " hostname mappings from CSV");

            // Log a few sample mappings for debugging
            if (LOGGER.isLoggable(Level.FINE) && !mapping.isEmpty()) {
                int count = 0;
                for (Map.Entry<String, String> entry : mapping.entrySet()) {
                    if (count < 5) {
                        LOGGER.fine("Sample mapping: " + entry.getKey() + " -> " + entry.getValue());
                        count++;
                    } else {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load hostname mapping from CSV: " + CSV_RESOURCE_PATH, e);
        }

        hostnameMapping = mapping;
        initialized = true;
    }

    /**
     * Parses a CSV line, handling quoted values.
     * @param line The CSV line to parse
     * @return Array of parsed values
     */
    private static String[] parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }

        java.util.List<String> values = new java.util.ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue.setLength(0);
            } else {
                currentValue.append(c);
            }
        }

        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    /**
     * Reloads the mapping from the CSV file.
     * Useful for testing or when the CSV file is updated.
     */
    public static void reloadMapping() {
        initialized = false;
        hostnameMapping = null;
        initializeMapping();
    }
}
