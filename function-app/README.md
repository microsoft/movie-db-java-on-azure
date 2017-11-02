# Deploy Java function to Azure using Maven plugin

## Introduction

This is a java function running in Azure Functions service that resizes images located in azure storage container `images-original` and outputs resized images to container `images-thumbnail`.

This java function can be easily deployed to Azure Functions with Maven plugin `azure-functions-maven-plugin`.

## Prerequisite

To deploy this function app, below info should be provided.

1. Resource group name.
2. Function app name.
3. Connection string of the image storage.

The three strings are specified by environment variables `COMMON_GROUP`, `FUNCTION_APP` and `STORAGE_CONNECTION_STRING` respectively. 

If you've run `provision.sh` successfully, these three env variables are already set properly for you. No extra operation is needed. Or else, you need to manually set them before running below commands.

## Commands to run

   ```shell
    cd function-app
    mvn clean package
    mvn azure-functions:deploy
   ```