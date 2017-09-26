# Deploy Java function to Azure using Maven plugin

## Introduction

This is a java function running in Azure Functions service that resizes images located in azure storage container `incontainer` and outputs resized images to container `outconatiner`.

This java function can be easily deployed to Azure Functions with Maven plugin `azure-functions-maven-plugin`.

## Prerequisite

1. The connection string of Azure storage account, which is the account storing images, has to be set to environment variable `STORAGE_CONNECTION_STRING`, before running below commands. If you've run `provision.sh` successfully, this variable is already set for you. The provisioning script also sets the enviroment variable `COMMON_GROUP`, which is the resource group that the app will be deployed to.

2. The web app name is specified by configuration property `appName` in pom file. It has to be globally unique.

## Commands to run

   ```shell
    cd azure-functions-java
    mvn clean package
    mvn azure-functions:deploy
   ```