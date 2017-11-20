#! /bin/bash
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See License.txt in the project root for license information.
#
#
# Provision resources in Azure

# Source library
source lib.sh

# Check whether script is running in a sub-shell
if [ "${BASH_SOURCE}" == "$0" ]; then
    log_error '"provision.sh" should be sourced. Use ". provision.sh --help" for detailed information.'
    exit 1
fi

# Check required tools. Exit if requirements aren't satisfied.
check_required_tools
[[ $? -ne 0 ]] && return 1

# Load config.json and export environment variables
load_config

# Parse command line arguments
parse_args "$@"
[[ $? -ne 0 ]] && return 1

# Check required environment variables
check_required_env_vars
[[ $? -ne 0 ]] && return 1

# Prefix resource group names with target environment
c_group=${TARGET_ENV}${COMMON_GROUP}${GROUP_SUFFIX}
e_us_group=${TARGET_ENV}${EAST_US_GROUP}${GROUP_SUFFIX}
w_eu_group=${TARGET_ENV}${WEST_EUROPE_GROUP}${GROUP_SUFFIX}
jenkins_group=${JENKINS_GROUP}${GROUP_SUFFIX}

print_banner 'Start provisioning shared resources...'
az group create -n ${c_group} -l ${EAST_US}
create_shared_resources ${c_group} ${MYSQL_ADMIN_USERNAME} ${MYSQL_PASSWORD}

print_banner 'Start provisioning resources in East US region...'
az group create -n ${e_us_group} -l ${EAST_US}
create_webapp ${e_us_group} westus
create_kubernetes ${e_us_group} ${ACS_NAME}

print_banner 'Start provisioning resources in West Europe region...'
az group create -n ${w_eu_group} -l ${WEST_EUROPE}
create_webapp ${w_eu_group} westeurope
create_kubernetes ${w_eu_group} ${ACS_NAME}

print_banner 'Start provisioning Kubernetes cluster for Jenkins if not exist...'
az group create -n ${jenkins_group} -l ${EAST_US}
create_kubernetes ${jenkins_group} ${ACS_NAME}

print_banner 'Wait until resource provisioning is finished...'
wait_till_kubernetes_created ${e_us_group} ${ACS_NAME}
[[ $? -ne 0 ]] && return 1

wait_till_kubernetes_created ${w_eu_group} ${ACS_NAME}
[[ $? -ne 0 ]] && return 1

wait_till_kubernetes_created ${jenkins_group} ${ACS_NAME}
[[ $? -ne 0 ]] && return 1

if [[ -n "$MS_CORP" ]]; then
  # For MS developers, all the VM provisioned will be applied with NSG rules to allow
  # access only from internal CORP network. This will block the access between the
  # VMs provisioned for the project, so Jenkins slaves will not be able to access
  # the ACS master node through SSH port.
  # This is a fix to this problem.
  allow_acs_nsg_access "Internet" "${e_us_group}"
  allow_acs_nsg_access "Internet" "${w_eu_group}"
  allow_acs_nsg_access "Internet" "${jenkins_group}"
fi

wait_till_deployment_created ${c_group} master
[[ $? -ne 0 ]] && return 1

print_banner 'Populating database...'
init_database ${c_group}

print_banner 'Creating secrets and config map in Kubernetes...'
export_database_details ${c_group}
create_secrets_in_kubernetes ${e_us_group} ${ACS_NAME}
create_secrets_in_kubernetes ${w_eu_group} ${ACS_NAME}

print_banner 'Deploy Jenkins cluster if not exist...'
deploy_jenkins ${jenkins_group} ${ACS_NAME}

# Set up environment variables for local dev environment
source dev_setup.sh "$@"

print_banner 'Provision completed'
