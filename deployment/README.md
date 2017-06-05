# Development Environment Setup

The following instructions contain the required steps to install the necessary utilities and setup your development environment in order to run the sample application which is detailed in the parent [README](../README.md) file.

## STEP 1 - Install the required developer utilities for your operating system ##

### If you are using a MacOS system ###

1. Install the **Azure CLI** using the instructions at https://docs.microsoft.com/en-us/cli/azure/install-azure-cli:

1. Install **kubectl** by running below command:

   ```shell
   sudo az acs kubernetes install-cli
   ```

1. Install **Homebrew** from https://brew.sh/

1. Install **jq** using Homebrew:

   ```shell
   brew install jq
   ```

1. Install **gettext** using Homebrew:

   ```shell
   brew install gettext
   brew link --force gettext
   ```

1. Install **maven** using Homebrew:

   ```shell
   brew install maven
   ```

### If you are using a Windows system ###

1. Install the **Azure CLI** using the instructions at https://docs.microsoft.com/en-us/cli/azure/install-azure-cli.

1. Install **kubectl** by running below Azure CLI command with administrator privilege:

   ```shell
   az acs kubernetes install-cli
   ```

1. Install [Chocolatey](https://chocolatey.org/).

1. Install **[Maven](http://maven.apache.org/)** using Chocolatey:

   ```shell
   choco install Maven
   ```

1. Install **jq** using Chocolatey:

   ```shell
   choco install jq
   ```

## STEP 2 - Clone the sample application and customize it for your environment ##

1. Open https://github.com/Microsoft/movie-db-java-on-azure in a web browser and create a private fork of the sample application.

1. Open a console window and clone your forked repo on your local system:

   ```shell
   git clone https://github.com/<your-github-id>/movie-db-java-on-azure
   ```

1. Open the `~/deployment/config.json` in a text editor:

   a. Locate the following configuration settings, and modify the `value` of the GITHUB_REPO_OWNER key/value pair with your GitHub account name:

      ```json
      "repo": [
         {
            "comment": "GitHub account name.",
            "key": "GITHUB_REPO_OWNER",
            "value": "<your-github-id>"
         },
         {
            "comment": "GitHub repository name.",
            "key": "GITHUB_REPO_NAME",
            "value": "movie-db-java-on-azure"
         }
      ],
      ```

   b. Locate the following configuration settings, and modify the `value` of the GROUP_SUFFIX key/value pair with a unique ID; for example "123456":

      ```json
      "optional": [
         {
            "comment": "Optional resource group suffix to avoid naming conflict.",
            "key": "GROUP_SUFFIX",
            "value": "<some-random-suffix>"
         }
      ]
      ```

   c. Save and close the `~/deployment/config.json` file.
      Suggest to commit these changes to GitHub so that it won't be lost.

1. Login to your Azure account and specify which subscription to use:

   ```shell
   az login
   az account set --subscription "<your-azure-subscription>"
   ```

   **NOTE**: You can use either a subscription name or id when specifying which subscription to use; to obtain a list of your subscriptions, type `az account list`.

1. Setup environment variables for passwords.
   ```shell
      export MYSQL_PASSWORD=<your-mysql-admin-password>
      export JENKINS_PASSWORD=<your-jenkins-password>
   ```

   `MYSQL_PASSWORD` will be used to create a MySQL database in Azure. It has to satisfy certain complexity.
   Default MySQL admin username is `AzureUser`. You can change it in `~/deployment/config.json`.

   `JENKINS_PASSWORD` will be used to deploy a Jenkins cluster in ACS. Jenkins admin username is `jenkins`.

<!--

**NOTE**: Follow the steps in the root-level README.md file instead of using the following steps.

1. Run the following command:

   ```bash
   source provision.sh
   ```

1. Wait for about 16 minutes till all resources are created.

   The IP of Jenkins server will be displayed at the end of the output. 

   During the installation, there might be prompt for your credential for elevated permission to install `kubectl`.

1. Go to the Jenkins server and login with username `jenkins` and password set in step 13.

1. Because our Repo is private right now, you will have to setup credentials to allow Jenkins enlist your repo. Click the pipeline job and configure it.

1. At the pipeline tab, add a new credential with your GitHub account and your personal access token.

   Refer to [GitHub document](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/) for creating your personal access token.

1. After configuration is saved, click "build now" to trigger the first deployment of web-app and data-app.

1. When deployment is done, go to Azure Portal to find the URL of web-app traffic manager.

   Open the URL in browser, then you will see the home page of the web-app.

For more information about using GitHub with Jenkins, see [How to Start Working with the GitHub Plugin for Jenkins](https://www.blazemeter.com/blog/how-start-working-github-plugin-jenkins) for details on how to enable Jenkins triggers every time changes are pushed to GitHub.

-->