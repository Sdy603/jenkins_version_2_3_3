# dx-data-sharer

## Introduction

This plugin is intended for customers of DX (https://getdx.com/). It shares Jenkins build data with DX.
It targets Jenkins `2.332.2` for compatibility with older controller installations.

To run this plugin locally:
- Install Jenkins: https://www.jenkins.io/doc/pipeline/tour/getting-started/
- Setup your developer environment: https://www.jenkins.io/doc/developer/tutorial/prepare/
- `mvn hpi:run -Dport=5000`
- Navigate to http://localhost:5000/jenkins

To generate a *.hpi file:
- `mvn package`

## Getting started
### Installing the Plugin
- Run your Jenkins instance. This involves finding `jenkins.war` (the location can vary, but if you installed with Homebrew, it is likely `/usr/local/Cellar/jenkins/2.451/libexec`), and running `Java -jar jenkins.war`

- Click "Manage Jenkins"
<img width="355" alt="Screenshot 2024-04-09 at 4 25 20 PM" src="https://github.com/get-dx/datacloud-jenkins/assets/44679211/be44257c-e484-44f5-8a13-85fe20e0df0b">

- Click "Plugins"
<img width="962" alt="Screenshot 2024-04-09 at 4 25 29 PM" src="https://github.com/get-dx/datacloud-jenkins/assets/44679211/848e7e31-aca5-47a7-b512-297470259148">

- Click "Advanced Settings"
<img width="351" alt="Screenshot 2024-04-09 at 4 25 39 PM" src="https://github.com/get-dx/datacloud-jenkins/assets/44679211/8dbbd35a-4ae9-4bda-b492-993ddac9b880">

- Click "Choose File" under "Deploy Plugin", and select the *.hpi file
<img width="870" alt="Screenshot 2024-04-09 at 4 25 51 PM" src="https://github.com/get-dx/datacloud-jenkins/assets/44679211/5b4cdf72-82ef-4aad-9576-0b9d57bb736b">

- Once the upload is complete, restart Jenkins
- To confirm the plugin is present, confirm `dx-data-sharer.jpi` is present in {jenkins_home}/plugins/
- To confirm the plugin is running, check the console ouput for a build. You should see the message: "Authentication token not found for key: dx_token". Proceed to the next step: Adding your DX API Key to Jenkins.

### Adding Your DX API Key to Jenkins
- Click “Manage Jenkins” in the menu on the left side of the homepage.
<img width="322" alt="Screenshot 2024-04-01 at 12 16 19 PM" src="https://github.com/mattilavan/jenkins-plugins/assets/44679211/670767a6-6acc-4da0-9596-e32b7e8dbcc5">

- Click “Manage Credentials” in the Security section
<img width="969" alt="Screenshot 2024-04-01 at 12 16 40 PM" src="https://github.com/mattilavan/jenkins-plugins/assets/44679211/ab07cd71-d6aa-4a55-87c8-349c196b538d">

- You will see your credentials, if you have any. Click on the store or domain you would like to add your DX API Key to.
- For "Kind", select “Secret Text”
- Choose your desired scope
- For “Secret”, add your API Key
- For “ID”, use the string "dx-api-token"
- For "Description", use “API Key for the DX API”
- Click “OK”

- Click “Add Credentials”.
<img width="343" alt="Screenshot 2024-04-01 at 12 25 13 PM" src="https://github.com/mattilavan/jenkins-plugins/assets/44679211/3b2be1f0-e341-4de3-bf65-0947408cce73">


### Configuring the Plugin
- Navigate to **Manage Jenkins → System**.
- In the **DX Data Sharing** section, configure the following:
  - **DX API Base URL** – Base URL for your DX instance, e.g. `https://dx.example.com`.

### Hostname Mapping (Automatic)

The plugin automatically maps the Jenkins server's hostname to a pipeline source using the `jenkins-server.csv` file:

- **Automatic Detection**: The plugin detects the Jenkins server's hostname automatically
- **CSV Mapping**: Maps hostnames to pipeline source descriptions (e.g., "APEX Central Engineering")
- **Default Fallback**: Uses "jenkins-unmapped" if no mapping is found
- **No Configuration Required**: Works out of the box with the included CSV file

For detailed information about hostname mapping, see [HOSTNAME_MAPPING.md](HOSTNAME_MAPPING.md).

## Issues

Report issues and enhancements in the [Jenkins issue tracker](https://issues.jenkins.io/).

## Contributing

[CONTRIBUTING](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

