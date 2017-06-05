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


def deployFunction() {
    sh """
        # Storage connection for images 
        storage_name=\$(az storage account list -g ${config.COMMON_GROUP} --query [2].name | tr -d '"')
        storage_conn_str=\$(az storage account show-connection-string -g ${config.COMMON_GROUP} -n \${storage_name} --query connectionString | tr -d '"')
        
        function_id=\$(az functionapp list -g ${config.COMMON_GROUP} --query [0].id | tr -d '"')
        az functionapp config appsettings set --ids \${function_id} --settings STORAGE_CONNECTION_STRING=\${storage_conn_str}
        az functionapp deployment source sync --ids \${function_id}
    """
}

def deployWebApp(String resGroup) {
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
        
        az webapp config container set --ids \${webapp_id} \\
                                      --docker-custom-image-name ${acrLoginServer}/web-app \\
                                      --docker-registry-server-url http://${acrLoginServer} \\
                                      --docker-registry-server-user ${acrUsername} \\
                                      --docker-registry-server-password ${acrPassword}
        az webapp config set --ids \${webapp_id} --linux-fx-version "DOCKER|${acrLoginServer}/web-app"
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
     
        # Create private container registry if not exist
        if [ -z "\$(kubectl get ns ${targetEnv} --ignore-not-found)" ]; then
          kubectl create ns ${targetEnv} --save-config
        fi
        secret_exist=\$(kubectl get secret ${acrLoginServer} --namespace=${targetEnv} --ignore-not-found)
        if [ -z "\${secret_exist}" ]; then
          kubectl create secret docker-registry ${acrLoginServer} --namespace=${targetEnv} \\
                                                                  --docker-server=${acrLoginServer} \\
                                                                  --docker-username=${acrUsername} \\
                                                                  --docker-password=${acrPassword} \\
                                                                  --docker-email=foo@foo.bar \\
                                                                  --save-config
        fi

        # Deploy data app
        export ACR_LOGIN_SERVER=${acrLoginServer}
        export DATA_APP_CONTAINER_PORT=${config.DATA_APP_CONTAINER_PORT}
        export TARGET_ENV=${targetEnv}
        envsubst < ./deployment/data-app/deploy.yaml | kubectl apply --namespace=${targetEnv} -f -
     
        # Wait until external IP is created for data app
        data_app_ip=\$(kubectl get svc data-app -o jsonpath={.status.loadBalancer.ingress[0].ip} --ignore-not-found --namespace=${targetEnv})
        while [ -z "\${data_app_ip}" ]
        do
          sleep 5
          data_app_ip=\$(kubectl get svc data-app -o jsonpath={.status.loadBalancer.ingress[0].ip} --ignore-not-found --namespace=${targetEnv})
        done
     
        # Update DNS name of public ip resource for data app
        ip_name=\$(az network public-ip list -g ${resGroup} --query "[?ipAddress=='\${data_app_ip}']" | python -c "import sys, json; print json.load(sys.stdin)[0]['name']")
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
