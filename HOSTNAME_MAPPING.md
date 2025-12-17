# Hostname Mapping for Pipeline Source

This Jenkins plugin now automatically maps the Jenkins server's hostname to a pipeline source using a CSV configuration file.

## How It Works

1. **Hostname Detection**: The plugin automatically detects the hostname of the Jenkins server using `InetAddress.getLocalHost().getHostName()`
2. **CSV Lookup**: It looks up the hostname in the `jenkins-server.csv` file located in `src/main/resources/`
3. **Pipeline Source**: The `pipeline_source` field in the payload is set to the mapped value from the CSV
4. **Fallback**: If no mapping is found, it defaults to `"jenkins-unmapped"`

## CSV Format

The CSV file should have the following format:
```csv
"hostname","pipeline_source_description"
"osj-ace-01-prd","APEX Central Engineering"
"osj-ace-01-tst","APEX Central Engineering"
```

## Configuration

- **CSV File Location**: `src/main/resources/jenkins-server.csv`
- **Default Value**: `"jenkins-unmapped"` (when hostname not found in CSV)
- **Auto-reload**: The mapping is loaded once when the plugin starts

## Logging

The plugin provides detailed logging for debugging:
- **FINE level**: Shows individual mappings and hostname lookups
- **INFO level**: Shows when mappings are loaded and when defaults are used
- **WARNING level**: Shows any errors in hostname detection or CSV parsing

## Example Output

When a build completes, you'll see logs like:
```
DX: Detected hostname: osj-ace-01-prd
DX: Using pipeline source: APEX Central Engineering
```

## Updating Mappings

To update the hostname mappings:
1. Modify the `jenkins-server.csv` file
2. Restart Jenkins or reload the plugin
3. The new mappings will be automatically loaded

## Troubleshooting

- **Hostname not detected**: Check that the Jenkins server can resolve its own hostname
- **CSV not loaded**: Verify the CSV file is in the correct location and has proper permissions
- **Default value used**: Check that the hostname in the CSV matches exactly (case-sensitive)
- **Parsing errors**: Ensure the CSV follows the quoted format: `"hostname","description"` 