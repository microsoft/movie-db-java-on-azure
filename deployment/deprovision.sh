#! /bin/bash
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See License.txt in the project root for license information.
#
#
# Utility script to de-provision Azure resources

# Source library
source lib.sh

# Check required tools. Exit if requirements aren't satisfied.
check_required_tools
[[ $? -ne 0 ]] && exit 1

# Load config.json and export environment variables
load_config

export TEARDOWN_NO_WAIT=true
# Parse command line arguments
parse_teardown_args "$@"
[[ $? -ne 0 ]] && exit 1

# Prefix resource group names with target environment
c_group=${TARGET_ENV}${COMMON_GROUP}${GROUP_SUFFIX}
e_us_group=${TARGET_ENV}${EAST_US_GROUP}${GROUP_SUFFIX}
w_eu_group=${TARGET_ENV}${WEST_EUROPE_GROUP}${GROUP_SUFFIX}
jenkins_group=${JENKINS_GROUP}${GROUP_SUFFIX}

# Delete resource groups in parallel
log_info "Start deleting resource group ${c_group}..."
az group delete -y -n ${c_group} --no-wait

log_info "\nStart deleting resource group ${e_us_group}..."
az group delete -y -n ${e_us_group} --no-wait

log_info "\nStart deleting resource group ${w_eu_group}..."
az group delete -y -n ${w_eu_group} --no-wait

log_info "\nStart deleting resource group ${jenkins_group}..."
az group delete -y -n ${jenkins_group} --no-wait

# Wait for completion if called with '--wait'
if [ "${TEARDOWN_NO_WAIT}" != "true" ]; then
  log_info "\nWait for delete completion..."

  az group wait -n ${c_group} --deleted
  az group wait -n ${e_us_group} --deleted
  az group wait -n ${w_eu_group} --deleted
  az group wait -n ${jenkins_group} --deleted

  log_info "\nAll deleted."
fi
