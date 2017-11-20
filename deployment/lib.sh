#! /bin/bash
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See License.txt in the project root for license information.
#
#
# Bash function library

##############################################################################
# Show help message
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
##############################################################################
function show_help()
{
  echo "
Usage:
    source [shell script] [options]
    . [shell script] [options]

Options:
    --mysql-password [value]    Password for MySQL database to be created.
    --jenkins-password [value]  Password for 'jenkins' account to be created in Jenkins cluster.
    --env [value]               Optional. Target environment.
                                Allow values: dev, test, prod. Default is 'dev'.
    --group-suffix [value]      Optional. Resource group suffix to avoid conflict. Default is empty.
    --github-owner [value]      Optional. GitHub repository owner. Typically it is your GitHub account name.
"
}

##############################################################################
# Show help message
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
##############################################################################
function show_teardown_help()
{
  echo "
Deprovision.sh will delete all Azure resources created by provision.sh.

Usage:
    bash deprovision.sh [options]
    ./deprovision.sh [options]

Options:
    --env [value]           Optional. Target environment to provision.
                            Allow values: dev, test, prod. Default is 'dev'.
    --group-suffix [value]  Optional. Suffix of provisioned resource groups. Default is empty.
    --wait                  Optional. If this options is specified, script will wait until all resource groups are deleted successfully.
                            By default, script will initiate the deletion of resource groups and exit immediately.
"
}

##############################################################################
# Parse arguments and setup environment variables
# Globals:
#   TARGET_ENV
#   MYSQL_PASSWORD
#   JENKINS_PASSWORD
#   GROUP_SUFFIX
#   GITHUB_REPO_OWNER
# Arguments:
#   All args from command line
# Returns:
#   None
##############################################################################
function parse_args()
{
  while :; do
    case "$(echo $1 | tr '[:upper:]' '[:lower:]')" in
      -h|-\?|--help)
        show_help
        return 1
        ;;
      --env)
        if [ -n "$2" ]; then
          case "$(echo $2 | tr '[:upper:]' '[:lower:]')" in
            dev) export TARGET_ENV=dev ;;
            test) export TARGET_ENV=test ;;
            prod) export TARGET_ENV=prod ;;
            *)
              log_error "Invalid argument for option \"--env\": $2"
              return 1
              ;;
          esac
          shift
        else
          log_error "\"--env\" requires an argument."
          return 1
        fi
        ;;
      --mysql-password)
        if [ -n "$2" ]; then
          export MYSQL_PASSWORD=$2
          shift
        else
          log_error "\"--mysql-password\" requires an argument."
          return 1
        fi
        ;;
      --jenkins-password)
        if [ -n "$2" ]; then
          export JENKINS_PASSWORD=$2
          shift
        else
          log_error "\"--jenkins-password\" requires an argument."
          return 1
        fi
        ;;
      --group-suffix)
        if [ -n "$2" ]; then
          export GROUP_SUFFIX=$2
          shift
        else
          log_error "\"--group-suffix\" requires an argument."
          return 1
        fi
        ;;
      --github-owner)
        if [ -n "$2" ]; then
          export GITHUB_REPO_OWNER=$2
          shift
        else
          log_error "\"--github-owner\" requires an argument."
          return 1
        fi
        ;;
      -?*)
        log_warning "Unknown option \"$1\""
        ;;
      *)
        break
    esac

    shift
  done
}

##############################################################################
# Parse arguments and setup environment variables
# Globals:
#   TARGET_ENV
#   GROUP_SUFFIX
# Arguments:
#   All args from command line
# Returns:
#   None
##############################################################################
function parse_teardown_args()
{
  while :; do
    case "$(echo $1 | tr '[:upper:]' '[:lower:]')" in
      -h|-\?|--help)
        show_teardown_help
        return 1
        ;;
      --env)
        if [ -n "$2" ]; then
          case "$(echo $2 | tr '[:upper:]' '[:lower:]')" in
            dev) export TARGET_ENV=dev ;;
            test) export TARGET_ENV=test ;;
            prod) export TARGET_ENV=prod ;;
            *)
              log_error "Invalid argument for option \"--env\": $2"
              return 1
              ;;
          esac
          shift
        else
          log_error "\"--env\" requires an argument."
          return 1
        fi
        ;;
      --group-suffix)
        if [ -n "$2" ]; then
          export GROUP_SUFFIX=$2
          shift
        else
          log_error "\"--group-suffix\" requires an argument."
          return 1
        fi
        ;;
      --wait)
        export TEARDOWN_NO_WAIT=false
        ;;
      -?*)
        log_warning "Unknown option \"$1\""
        ;;
      *)
        break
    esac

    shift
  done
}

##############################################################################
# Check whether tool is installed
# Globals:
#   None
# Arguments:
#   tool_name
#   test_command
# Returns:
#   None
##############################################################################
function check_tool()
{
  local tool_name=$1
  local test_command=$2
  ${test_command} > /dev/null 2>&1
  if [ $? != 0 ]; then
    log_error "\"${tool_name}\" not found. Please install \"${tool_name}\" before running this script."
    return 1
  fi
}

##############################################################################
# Check whether all required tools are installed
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
##############################################################################
function check_required_tools()
{
  check_tool 'Java SDK' 'javac -version'
  [[ $? -ne 0 ]] && return 1

  check_tool 'Maven' 'mvn --version'
  [[ $? -ne 0 ]] && return 1

  check_tool 'Azure CLI 2.0' 'az --version'
  [[ $? -ne 0 ]] && return 1

  check_tool 'docker' 'docker --version'
  [[ $? -ne 0 ]] && return 1

  check_tool 'jq' 'jq -h'
  [[ $? -ne 0 ]] && return 1

  check_tool 'gettext' 'envsubst -h'
  [[ $? -ne 0 ]] && return 1

  check_tool 'kubectl' 'kubectl'
  [[ $? -ne 0 ]] && return 1

  return 0
}

##############################################################################
# Check whether all required environment variables are set up
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
##############################################################################
function check_required_env_vars()
{
  if [ -z "${MYSQL_PASSWORD}" ]; then
    log_error 'Environment variable MYSQL_PASSWORD not found. Either setup environment variable MYSQL_PASSWORD or pass it with "--mysql-password" option.'
    return 1
  fi

  if [ -z "${JENKINS_PASSWORD}" ]; then
    log_error 'Environment variable JENKINS_PASSWORD not found. Either setup environment variables JENKINS_PASSWORD or pass it with "--jenkins-password" option.'
    return 1
  fi
}

##############################################################################
# Read all key-value pairs from config.json and export as environment variables
# Globals:
#   Environment variables set in config.json
# Arguments:
#   None
# Returns:
#   None
##############################################################################
function load_config()
{
  local keys=( $(jq -r '.[] | values[] | select(.value != "").key' config.json) )
  local values=( $(jq -r '.[] | values[] | select(.value != "").value' config.json) )

  local total=${#keys[*]}
  for ((i=0; i < $((total)); i++))
  do
    #On Windows, there seems to be special line ending characters. Remove them.
    local key=${keys[$i]}
    key=${key//$'\n'/}
    key=${key//$'\r'/}
    local value=${values[$i]}
    value=${value//$'\n'/}
    value=${value//$'\r'/}
    export ${key}=${value}
  done
}

##############################################################################
# Create shared resources with master ARM template
# Globals:
#   None
# Arguments:
#   resource_group
#   username: Admin login username for MySQL server
#   password: Admin login password for MySQL server
# Returns:
#   None
##############################################################################
function create_shared_resources()
{
  local resource_group=$1
  local username=$2
  local password=$3
  az group deployment create -g ${resource_group} --template-file ./arm/master.json \
                            --parameters "{\"administratorLogin\": {\"value\": \"${username}\"},\"administratorLoginPassword\": {\"value\": \"${password}\"}}" \
                            --no-wait
}

##############################################################################
# Wait for the completion of ARM template deployment
# Globals:
#   None
# Arguments:
#   resource_group
#   deployment_name
# Returns:
#   None
##############################################################################
function wait_till_deployment_created()
{
   local resource_group=$1
   local deployment_name=$2
   az group deployment wait -g ${resource_group} -n ${deployment_name} --created
   if [ $? != 0 ]; then
    log_error "Something is wrong when provisioning resources in resource group \"${resource_group}\". Please check out logs in Azure Portal."
    return 1
   fi
}

##############################################################################
# Create linux container web app
# Globals:
#   None
# Arguments:
#   resource_group
#   location
# Returns:
#   None
##############################################################################
function create_webapp()
{
  local resource_group=$1
  local location=$2
  az group deployment create -g ${resource_group} --template-file ./arm/linux-webapp.json \
                            --parameters "{\"location\": {\"value\": \"${location}\"}}" \
                            --query "{id:id,name:name,provisioningState:properties.provisioningState,resourceGroup:resourceGroup}"

  # Config to disable built-in image, which will be rejected by the jenkins app service plugin.
  # Use a docker hub image instead to provision. It will be replaced by a custom image during deploy.
  local name=$(az resource list -g ${resource_group} --resource-type Microsoft.Web/sites --query [0].name | tr -d '"')
  az webapp config set -g ${resource_group} -n ${name} --linux-fx-version "DOCKER|NGINX" \
                      --query "{id:id,name:name,provisioningState:properties.provisioningState,resourceGroup:resourceGroup}"
}

##############################################################################
# Create Kubernetes cluster without waiting if it doesn't exist
# Globals:
#   None
# Arguments:
#   resource_group
#   acs_name
# Returns:
#   None
##############################################################################
function create_kubernetes()
{
  local resource_group=$1
  local acs_name=$2
  if [ -z "$(az acs show -g ${resource_group} -n ${acs_name})" ]; then
    az acs create --orchestrator-type=kubernetes -g ${resource_group} -n ${acs_name} \
                  --generate-ssh-keys --agent-count 1 --no-wait
  fi
}

##############################################################################
# Wait for the completion of Kubernetes cluster creation
# Globals:
#   None
# Arguments:
#   resource_group
#   acs_name
# Returns:
#   None
##############################################################################
function wait_till_kubernetes_created() {
  local resource_group=$1
  local acs_name=$2
  az acs wait -g ${resource_group} -n ${acs_name} --created
  if [ $? != 0 ]; then
    log_error "Something is wrong when provisioning Kubernetes in resource group \"${resource_group}\". Please check out logs in Azure Portal."
    return 1
  fi
}

##############################################################################
# Create ConfigMap and Secrets in Kubernetes cluster
# Globals:
#   TARGET_ENV
#   MYSQL_ENDPOINT
#   MYSQL_USERNAME
#   MYSQL_PASSWORD
# Arguments:
#   resource_group
#   acs_name
# Returns:
#   None
##############################################################################
function create_secrets_in_kubernetes() {
  local resource_group=$1
  local acs_name=$2

  az acs kubernetes get-credentials -g ${resource_group} -n ${acs_name}

  if [ -z "$(kubectl get ns ${TARGET_ENV} --ignore-not-found)" ]; then
    kubectl create ns ${TARGET_ENV} --save-config
  fi
  kubectl config set-context $(kubectl config current-context) --namespace=${TARGET_ENV}

  if [ -n "$(kubectl get secret my-secrets --ignore-not-found)" ]; then
    kubectl delete secret my-secrets
  fi
  kubectl create secret generic my-secrets --type=string  --save-config \
                                          --namespace=${TARGET_ENV} \
                                          --from-literal=mysqlEndpoint=${MYSQL_ENDPOINT} \
                                          --from-literal=mysqlUsername=${MYSQL_USERNAME} \
                                          --from-literal=mysqlPassword=${MYSQL_PASSWORD}
}

##############################################################################
# Deploy Jenkins if it doesn't exist
# Globals:
#   GITHUB_REPO_OWNER
#   GITHUB_REPO_NAME
#   JENKINS_PASSWORD
#   GROUP_SUFFIX
# Arguments:
#   resource_group
#   acs_name
# Returns:
#   None
##############################################################################
function deploy_jenkins()
{
  create_secrets_in_jenkins_kubernetes $1 $2

  if [ -z "$(kubectl get deploy jenkins --ignore-not-found --namespace=jenkins)" ]; then
    kubectl apply -f ./jenkins/jenkins-master.yaml
  fi

  # Check existence of Jenkins service
  check_jenkins_readiness
}

##############################################################################
# Add network security group rule to allow the given IP to access the ACS
# master SSH service.
# Globals:
#   None
# Arguments:
#   source_ip
#   resource_group
# Returns:
#   None
##############################################################################
function allow_acs_nsg_access()
{
  local source_ip=$1
  local resource_group=$2

  local nsgs=($(az network nsg list --resource-group "$resource_group" --query '[].name' --output tsv | grep -e "^k8s-master-"))
  local port_range=22
  if [ "$source_ip" = Internet ]; then
    # web job deletes the rule if the port is set to 22 for wildcard internet access
    port_range="21-23"
  fi
  for nsg in "${nsgs[@]}"; do
    local name="allow_$source_ip"
    # used a fixed priority here
    local max_priority="$(az network nsg rule list -g "$resource_group" --nsg-name "$nsg" --query '[].priority' --output tsv | sort -n | tail -n1)"
    local priority="$(expr "$max_priority" + 50)"
    log_info "Add allow $source_ip rules to NSG $nsg in resource group $resource_group, with priority $priority"
    az network nsg rule create --priority "$priority" --destination-port-ranges "$port_range" --resource-group "$resource_group" \
        --nsg-name "$nsg" --name "$name" --source-address-prefixes "$source_ip"
    #az network nsg rule create --priority "$priority" --destination-port-ranges 22 --resource-group "$resource_group" \
    #    --nsg-name "$nsg" --name "$name" --source-address-prefixes "$source_ip"
  done
}

##############################################################################
# Create secrets in Kubernetes for Jenkins
# Globals:
#   None
# Arguments:
#   resource_group
#   acs_name
# Returns:
#   None
##############################################################################
function create_secrets_in_jenkins_kubernetes() {
  local resource_group=$1
  local acs_name=$2

  az acs kubernetes get-credentials -g ${resource_group} -n ${acs_name}

  if [ -z "$(kubectl get ns jenkins --ignore-not-found)" ]; then
    kubectl create ns jenkins --save-config
  fi
  kubectl config set-context $(kubectl config current-context) --namespace=jenkins

  if [ -n "$(kubectl get secret my-secrets --ignore-not-found)" ]; then
    kubectl delete secret my-secrets
  fi
  kubectl create secret generic my-secrets --save-config \
                                    --from-literal=jenkinsPassword=${JENKINS_PASSWORD} \
                                    --from-file=sshKey=${HOME}/.ssh/id_rsa

  if [ -n "$(kubectl get secret kube-config --ignore-not-found)" ]; then
    kubectl delete secret kube-config
  fi
  kubectl create secret generic kube-config --from-file=config=${HOME}/.kube/config

  if [ -n "$(kubectl get configMap my-config --ignore-not-found)" ]; then
    kubectl delete configmap my-config
  fi
  kubectl create configmap my-config --save-config \
                                    --from-literal=githubRepoOwner=${GITHUB_REPO_OWNER} \
                                    --from-literal=githubRepoName=${GITHUB_REPO_NAME} \
                                    --from-literal=groupSuffix=${GROUP_SUFFIX}
}

##############################################################################
# Check whether Jenkins is ready for access
# Globals:
#   None
# Arguments:
#   None
# Returns:
#   None
##############################################################################
function check_jenkins_readiness()
{
  while [ 1 ]
  do
    jenkins_ip=$(kubectl get svc -o jsonpath={.items[*].status.loadBalancer.ingress[0].ip})
    if [ -n "${jenkins_ip}" ]; then
      break;
    fi
    sleep 5
  done
  echo Jenkins is ready at http://${jenkins_ip}/.
}

##############################################################################
# Export Jenkins URL as environment variables
# Globals:
#   JENKINS_URL
# Arguments:
#   resource_group
#   acs_name
# Returns:
#   None
##############################################################################
function export_jenkins_url()
{
  local resource_group=$1
  local acs_name=$2
  az acs kubernetes get-credentials -g ${resource_group} -n ${acs_name}
  export JENKINS_URL=$(kubectl get svc -o jsonpath={.items[*].status.loadBalancer.ingress[0].ip} --namespace=jenkins)
}

##############################################################################
# Export Azure Container Registry information as environment variables
# Globals:
#   ACR_NAME
#   ACR_USERNAME
#   ACR_PASSWORD
#   ACR_LOGIN_SERVER
# Arguments:
#   resource_group
# Returns:
#   None
##############################################################################
function export_acr_details()
{
  local resource_group=$1
  export ACR_NAME=$(az acr list -g ${resource_group} --query [0].name | tr -d '"')
  if [ -z "${ACR_NAME}" ]; then
    echo No Azure Container Registry found. Exit...
    exit 1
  fi
  export ACR_USERNAME=$(az acr credential show -g ${resource_group} -n ${ACR_NAME} --query username | tr -d '"')
  export ACR_PASSWORD=$(az acr credential show -g ${resource_group} -n ${ACR_NAME} --query passwords[0].value | tr -d '"')
  export ACR_LOGIN_SERVER=$(az acr show -g ${resource_group} -n ${ACR_NAME} --query loginServer | tr -d '"')
}

##############################################################################
# Export MySQL server information as environment variables
# Globals:
#   MYSQL_USERNAME
#   MYSQL_SERVER_ENDPOINT
#   MYSQL_ENDPOINT
# Arguments:
#   resource_group
# Returns:
#   None
##############################################################################
function export_database_details()
{
  local resource_group=$1
  local server_name=$(az mysql server list -g ${resource_group} --query [0].name | tr -d '"')
  local username=$(az mysql server show -g ${resource_group} -n ${server_name} --query administratorLogin | tr -d '"')
  local endpoint=$(az mysql server show -g ${resource_group} -n ${server_name} --query fullyQualifiedDomainName | tr -d '"')
  local database_name=$(az mysql db list -g ${resource_group} --server-name ${server_name} --query [].name | jq -r '. - ["mysql","sys","performance_schema","information_schema"] | .[0]')

  export MYSQL_USERNAME=${username}@${server_name}
  export MYSQL_SERVER_ENDPOINT=jdbc:mysql://${endpoint}:3306/?serverTimezone=UTC
  export MYSQL_ENDPOINT=jdbc:mysql://${endpoint}:3306/${database_name}?serverTimezone=UTC
}

##############################################################################
# Populate initial data set to MySQL database
# Globals:
#   None
# Arguments:
#   resource_group
# Returns:
#   None
##############################################################################
function init_database()
{
  local resource_group=$1
  export_database_details ${resource_group}
  cd ../database; mvn sql:execute; cd ../deployment
}

##############################################################################
# Export Redis server information as environment variables
# Globals:
#   REDIS_HOST
#   REDIS_PASSWORD
# Arguments:
#   resource_group
# Returns:
#   None
##############################################################################
function export_redis_details()
{
  local resource_group=$1
  local redis_name=$(az redis list -g ${resource_group} --query [0].name | tr -d '"')

  export REDIS_HOST=$(az redis show -g ${resource_group} -n ${redis_name} --query hostName | tr -d '"')
  export REDIS_PASSWORD=$(az redis list-keys -g ${resource_group} -n ${redis_name} --query primaryKey | tr -d '"')
}

##############################################################################
# Export image storage account information as environment variables
# Globals:
#   STORAGE_CONNECTION_STRING
# Arguments:
#   resource_group
# Returns:
#   None
##############################################################################
function export_image_storage()
{
  local resource_group=$1
  storage_name=$(az storage account list -g ${resource_group} --query [2].name | tr -d '"')
  export STORAGE_CONNECTION_STRING=$(az storage account show-connection-string -g ${resource_group} -n ${storage_name} --query connectionString | tr -d '"')
  export FUNCTION_APP=$(az functionapp list -g ${resource_group} --query [0].repositorySiteName | tr -d '"')
}

##############################################################################
# Export web-app information as environment variables
# Globals:
#   WEBAPP_NAME
#   WEBAPP_PLAN
# Arguments:
#   resource_group
# Returns:
#   None
##############################################################################
function export_webapp_details()
{
  local resource_group=$1
  local prefix=$2
  export ${prefix}_WEBAPP_NAME=$(az resource list -g ${resource_group} --resource-type Microsoft.Web/sites --query [0].name | tr -d '"')
  export ${prefix}_WEBAPP_PLAN=$(az appservice plan list -g ${resource_group} --query [0].name | tr -d '"')
}

##############################################################################
# Export data-app IP as environment variables
# Globals:
#   DATA_API_URL
# Arguments:
#   namespace
#   resource_group
# Returns:
#   None
##############################################################################
function export_data_api_url()
{
  local namespace=$1
  local resource_group=$2
  local k8_context=$(az acs list -g ${resource_group} --query [0].masterProfile.dnsPrefix | tr '[:upper:]' '[:lower:]' | tr -d '"')
  kubectl config use-context ${k8_context} > /dev/null
  export DATA_API_URL=$(kubectl get services -o jsonpath={.items[*].status.loadBalancer.ingress[0].ip} --namespace=${namespace})
}

##############################################################################
# Print string in specified color
# Globals:
#   None
# Arguments:
#   color
#   info
# Returns:
#   None
##############################################################################
function log_with_color()
{
  local color=$1
  local no_color='\033[0m'
  local info=$2
  echo -e "${color}${info}${no_color}"
}

##############################################################################
# Print information string in green color
# Globals:
#   None
# Arguments:
#   info
# Returns:
#   None
##############################################################################
function log_info()
{
  local info=$1
  local green_color='\033[0;32m'
  log_with_color "${green_color}" "${info}"
}

##############################################################################
# Print warning string in yellow color
# Globals:
#   None
# Arguments:
#   info
# Returns:
#   None
##############################################################################
function log_warning()
{
  local info=$1
  local yellow_color='\033[0;33m'
  log_with_color "${yellow_color}" "[Warning] ${info}"
}

##############################################################################
# Print error string in green color
# Globals:
#   None
# Arguments:
#   info
# Returns:
#   None
##############################################################################
function log_error()
{
  local info=$1
  local red_color='\033[0;31m'
  log_with_color "${red_color}" "[Error] ${info}"
}

##############################################################################
# Print activity banner
# Globals:
#   None
# Arguments:
#   info
# Returns:
#   None
##############################################################################
function print_banner()
{
  local info=$1
  log_info ''
  log_info '********************************************************************************'
  log_info "* ${info}"
  log_info '********************************************************************************'
}
