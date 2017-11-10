node {
  stage('Checkout Source') {
    checkout scm
  }

  def azureUtil = load './deployment/jenkins/azureutil.groovy'
  def branchName = scm.branches[0].name.split('/').last()
  def targetEnv = (branchName in ['test','prod']) ? branchName : 'dev'

  stage('Prepare') {
    echo "Target environment is: ${targetEnv}"
    azureUtil.prepareEnv(targetEnv)
  }

  stage('Build') {
    sh("cd data-app; mvn compile; cd ..")
    sh("cd web-app; mvn compile; cd ..")
  }

  stage('Test') {
    sh("cd data-app; mvn test; cd ..")
    sh("cd web-app; mvn test; cd ..")
  }

  stage('Publish Docker Image') {
    withEnv(["ACR_NAME=${azureUtil.acrName}", "ACR_LOGIN_SERVER=${azureUtil.acrLoginServer}", "ACR_USERNAME=${azureUtil.acrUsername}", "ACR_PASSWORD=${azureUtil.acrPassword}"]) {
      sh("cd data-app; mvn package docker:build -DpushImage -DskipTests; cd ..")
      sh("cd web-app; mvn package docker:build -DpushImage -DskipTests; cd ..")
    }
  }

  stage('Deploy') {
    // Deploy function app
    dir('function-app') {
        azureUtil.deployFunctionApp()
    }

    // Deploy data app
    withEnv(["ACR_NAME=${azureUtil.acrName}", "ACR_LOGIN_SERVER=${azureUtil.acrLoginServer}", "ACR_USERNAME=${azureUtil.acrUsername}", "ACR_PASSWORD=${azureUtil.acrPassword}"]) {
      azureUtil.deployDataApp(targetEnv, azureUtil.config.EAST_US_GROUP)
      azureUtil.deployDataApp(targetEnv, azureUtil.config.WEST_EUROPE_GROUP)
    }

    // Deploy web app
    dir('web-app/target') {
        azureUtil.deployWebApp(azureUtil.config.EAST_US_GROUP, "docker/Dockerfile")
        azureUtil.deployWebApp(azureUtil.config.WEST_EUROPE_GROUP, "docker/Dockerfile")
    }
  }
}
