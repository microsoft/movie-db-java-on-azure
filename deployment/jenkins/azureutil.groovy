/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

def prepareEnv(String targetEnv) {
    /**
     * Parse config.json
     */
    def rawConfig = jsonParse(readFile('./deployment/config.json'))
    this.config = [:]
    for (category in rawConfig) {
        for (item in category.value) {
            this.config.put(item.key, item.value)
        }
    }

    if (env.GROUP_SUFFIX != null) {
        this.config['GROUP_SUFFIX'] = "${env.GROUP_SUFFIX}"
    }

    this.config['COMMON_GROUP'] = "${targetEnv}${config.COMMON_GROUP}${config.GROUP_SUFFIX}"
    this.config['EAST_US_GROUP'] = "${targetEnv}${config.EAST_US_GROUP}${config.GROUP_SUFFIX}"
    this.config['WEST_EUROPE_GROUP'] = "${targetEnv}${config.WEST_EUROPE_GROUP}${config.GROUP_SUFFIX}"

    /**
     * Azure CLI login
     */
    sh '''
        client_id=$(cat /etc/kubernetes/azure.json | python -c "import sys, json; print json.load(sys.stdin)['aadClientId']")
        client_secret=$(cat /etc/kubernetes/azure.json | python -c "import sys, json; print json.load(sys.stdin)['aadClientSecret']")
        tenant_id=$(cat /etc/kubernetes/azure.json | python -c "import sys, json; print json.load(sys.stdin)['tenantId']")
        az login --service-principal -u ${client_id} -p ${client_secret} --tenant ${tenant_id}
    '''

    this.acrName = sh(
            script: "az acr list -g ${config.COMMON_GROUP} --query [0].name | tr -d '\"'",
            returnStdout: true
    ).trim()

    if (this.acrName.length() == 0) {
        error('Azure Container Registry not found.')
    }

    this.acrLoginServer = sh(
            script: "az acr show -g ${config.COMMON_GROUP} -n ${acrName} --query loginServer | tr -d '\"'",
            returnStdout: true
    ).trim()
    this.acrUsername = sh(
            script: "az acr credential show -g ${config.COMMON_GROUP} -n ${acrName} --query username | tr -d '\"'",
            returnStdout: true
    ).trim()
    this.acrPassword = sh(
            script: "az acr credential show -g ${config.COMMON_GROUP} -n ${acrName} --query passwords[0].value | tr -d '\"'",
            returnStdout: true
    ).trim()
}

def deployFunctionApp() {
    def appName = sh(
            script: "az functionapp list -g ${config.COMMON_GROUP} --query [0].repositorySiteName | tr -d '\"'",
            returnStdout: true
    ).trim()

    sh """
        export COMMON_GROUP=${config.COMMON_GROUP}
        export FUNCTION_APP=${appName}
        mvn clean package
    """

    azureFunctionAppPublish azureCredentialsId: 'azure-sp', resourceGroup: config.COMMON_GROUP, appName: appName, filePath: '**/*.jar,**/*.json', sourceDirectory: "target/azure-functions/${appName}"
}

def deployWebApp(String resGroup, String dockerFilePath) {
    def appName = sh(
            script: "az webapp list -g ${resGroup} --query [0].name | tr -d '\"'",
            returnStdout: true
    ).trim()

    azureWebAppPublish appName: appName, azureCredentialsId: 'azure-sp', dockerFilePath: dockerFilePath, dockerImageName: "${this.acrName}.azurecr.io/web-app", dockerImageTag: '', dockerRegistryEndpoint: [credentialsId: 'acr', url: "https://${this.acrName}.azurecr.io"], filePath: '', publishType: 'docker', resourceGroup: resGroup, slotName: '', sourceDirectory: '', targetDirectory: ''

    sh """
        data_api_endpoint=\$(az network traffic-manager profile list -g ${config.COMMON_GROUP} --query [0].dnsConfig.fqdn | tr -d '"')
        webapp_id=\$(az resource list -g ${resGroup} --resource-type Microsoft.Web/sites --query [0].id | tr -d '"')

        # Storage connection for images
        storage_name=\$(az storage account list -g ${config.COMMON_GROUP} --query [2].name | tr -d '"')
        storage_conn_str=\$(az storage account show-connection-string -g ${config.COMMON_GROUP} -n \${storage_name} --query connectionString | tr -d '"')

        # Redis credentials
        redis_name=\$(az redis list -g ${config.COMMON_GROUP} --query [0].name | tr -d '"')
        redis_host=\$(az redis show -g ${config.COMMON_GROUP} -n \${redis_name} --query hostName | tr -d '"')
        redis_password=\$(az redis list-keys -g ${config.COMMON_GROUP} -n \${redis_name} --query primaryKey | tr -d '"')

        az webapp config appsettings set --ids \${webapp_id} \\
                                        --settings  DATA_API_URL=\${data_api_endpoint} \\
                                                    PORT=${config.WEB_APP_CONTAINER_PORT} \\
                                                    WEB_APP_CONTAINER_PORT=${config.WEB_APP_CONTAINER_PORT} \\
                                                    STORAGE_CONNECTION_STRING=\${storage_conn_str} \\
                                                    ORIGINAL_IMAGE_CONTAINER=${config.ORIGINAL_IMAGE_CONTAINER} \\
                                                    THUMBNAIL_IMAGE_CONTAINER=${config.THUMBNAIL_IMAGE_CONTAINER} \\
                                                    REDIS_HOST=\${redis_host} \\
                                                    REDIS_PASSWORD=\${redis_password}
        az webapp restart --ids \${webapp_id}

        # Add web-app endpoint to traffic manager
        traffic_manager_name=\$(az resource list -g ${config.COMMON_GROUP} --resource-type Microsoft.Network/trafficManagerProfiles --query [1].name | tr -d '"')
        if [ -z "\$(az network traffic-manager endpoint show -g ${config.COMMON_GROUP} --profile-name \${traffic_manager_name} -n web-app-${resGroup} --type azureEndpoints --query id)" ]; then
          az network traffic-manager endpoint create -g ${config.COMMON_GROUP} --profile-name \${traffic_manager_name} \\
                                                    -n web-app-${resGroup} --type azureEndpoints --target-resource-id \${webapp_id}
        fi
    """
}

def deployDataApp(String targetEnv, String resGroup) {
    sh """
        # Change context to target Kubernetes cluster
        context_name=\$(az acs list -g ${resGroup} --query [0].masterProfile.dnsPrefix | tr '[:upper:]' '[:lower:]' | tr -d '"')
        kubectl config use-context \${context_name}

        # Deploy data app
        export DATA_APP_CONTAINER_PORT=${config.DATA_APP_CONTAINER_PORT}
        export TARGET_ENV=${targetEnv}
        cd data-app
        mvn clean fabric8:resource

        cat target/fabric8/namespace.yml
        cat target/fabric8/secrets.yml
        cat target/fabric8/deployment.yml
        cat target/fabric8/service.yml

        # Jenkins plugin doesn't support apply namespace
        kubectl apply -f target/fabric8/namespace.yml
    """

    acsDeploy azureCredentialsId: 'azure-sp', configFilePaths: 'data-app/target/fabric8/deployment.yml,data-app/target/fabric8/service.yml', containerService: 'acs | Kubernetes', enableConfigSubstitution: true, resourceGroupName: resGroup, sshCredentialsId: 'acs-ssh'

    sh """
        # Check whether there is any redundant IP address
        ip_count=\$(az network public-ip list -g ${resGroup} --query "[?tags.service=='${targetEnv}/data-app'] | length([*])")
        if [ \${ip_count} -gt 1 ]; then
          echo Only one IP address is allowed for data-app. More than one IP addresses are found.
          echo Please check whether there is any unused resource.
          exit 1
        fi

        # Wait until external IP is created for data app
        while [ 1 ]
        do
          ip_name=\$(az network public-ip list -g ${resGroup} --query "[?tags.service=='${targetEnv}/data-app'] | [0].name" | tr -d '"')
          if [ -n "\${ip_name}" ]; then
            break
          fi
          sleep 5
        done

        # Update DNS name of public ip resource for data app
        ip_resource_guid=\$(az network public-ip show -n \${ip_name} -g ${resGroup} --query resourceGuid | tr -d '"')
        az network public-ip update -g ${resGroup} -n \${ip_name} --dns-name data-app-\${ip_resource_guid} --allocation-method Static

        # Add data app to traffic manager
        ip_resource_id=\$(az network public-ip show -g ${resGroup} -n \${ip_name} --query id | tr -d '"')
        traffic_manager_name=\$(az resource list -g ${config.COMMON_GROUP} --resource-type Microsoft.Network/trafficManagerProfiles --query [0].name | tr -d '"')
        endpoint_name=\$(az network traffic-manager endpoint show -g ${config.COMMON_GROUP} -n data-app-${resGroup} --profile-name \${traffic_manager_name} --type azureEndpoints | tr -d '"')
        if [ -z "\${endpoint_name}" ]; then
          az network traffic-manager endpoint create -g ${config.COMMON_GROUP} --profile-name \${traffic_manager_name} -n data-app-${resGroup} --type azureEndpoints --target-resource-id \${ip_resource_id}
        else
          az network traffic-manager endpoint update -g ${config.COMMON_GROUP} --profile-name \${traffic_manager_name} -n data-app-${resGroup} --type azureEndpoints --target-resource-id \${ip_resource_id}
        fi
    """
}

return this
