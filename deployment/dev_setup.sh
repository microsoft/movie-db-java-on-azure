#! /bin/bash
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See License.txt in the project root for license information.
#
#
# Setup environment variables for development

# Source library
source lib.sh

# Check whether script is running in a sub-shell
if [ "${BASH_SOURCE}" == "$0" ]; then
    log_error '"dev_setup.sh" should be sourced. Run "source dev_setup.sh" or ". dev_setup.sh"'
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

# Hard code target env to 'dev'
export TARGET_ENV=dev

# Prefix resource group name with 'dev'
export COMMON_GROUP=${TARGET_ENV}${COMMON_GROUP}${GROUP_SUFFIX}
export EAST_US_GROUP=${TARGET_ENV}${EAST_US_GROUP}${GROUP_SUFFIX}
export WEST_EUROPE_GROUP=${TARGET_ENV}${WEST_EUROPE_GROUP}${GROUP_SUFFIX}
export JENKINS_GROUP=${JENKINS_GROUP}${GROUP_SUFFIX}

export_acr_details ${COMMON_GROUP}

export_database_details ${COMMON_GROUP}

export_redis_details ${COMMON_GROUP}

export_image_storage ${COMMON_GROUP}

export_webapp_details ${EAST_US_GROUP} EAST_US
export_webapp_details ${WEST_EUROPE_GROUP} WEST_EUROPE

export_jenkins_url ${JENKINS_GROUP} ${ACS_NAME}

export_data_api_url ${TARGET_ENV} ${EAST_US_GROUP}
