/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

import groovy.json.JsonSlurper
import hudson.model.*
import jenkins.model.*
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.tasks.Mailer.UserProperty
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement
import org.csanchez.jenkins.plugins.kubernetes.*
import org.csanchez.jenkins.plugins.kubernetes.volumes.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.microsoft.azure.util.AzureCredentials

/**
 * Set up security for the Jenkins instance with below configuration.
 * Security Realm: Jenkins' own user database and do not allow sign up
 * Authorization: Logged-in users can do anything and read access is disabled for anonymous user
 */
void setupSecurity() {
    def instance = Jenkins.getInstance()
    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.setAllowAnonymousRead(false)
    def realm = new HudsonPrivateSecurityRealm(false, false, null)
    instance.setAuthorizationStrategy(strategy)
    instance.setSecurityRealm(realm)
    instance.save()
}

/**
 * Create user
 */
void createUser(String user_name, String password = '', String full_name = '', String email = 'jenkins@foo.bar') {
    def user = User.get(user_name)
    user.setFullName(full_name)
    def email_param = new UserProperty(email)
    user.addProperty(email_param)
    def pw_param = Details.fromPlainPassword(password)
    user.addProperty(pw_param)
    user.save()
}

/**
 * Create new pipelines
 */
void createPipeline(String githubRepo, String envName, String branchName) {
    def workspace = new File('.')
    def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
    try {
        new DslScriptLoader(jobManagement).runScript("""
            pipelineJob("movie-db-pipeline-for-${envName}") {
                description "Pipeline for ${envName} environment."
                scm {
                    git("${githubRepo}","*/${branchName}")
                }
                triggers {
                    githubPush()
                }
                definition {
                    cpsScm {
                        scm {
                            github("${githubRepo}","**/${branchName}")
                        }
                        scriptPath('Jenkinsfile')
                    }
                }
            }
            """)
    } catch (Exception e) {
        println("Fail to create/update pipeline for ${envName} environment.")
        println(e.toString())
        println(e.getMessage())
        println(e.getStackTrace())
    }
}

/**
 * Set number of executors in Jenkins
 */
void setExecutorNum(int num) {
    def instance = Jenkins.getInstance()
    instance.setNumExecutors(num)
    instance.save()
}

void addKubeCredential(String credentialId) {
    def kubeCredential = new ServiceAccountCredential(CredentialsScope.GLOBAL, credentialId, 'Kubernetes service account')
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), kubeCredential)
}

void addACRCredential(String credentialId, String configFile) {
    String content = new File(configFile).text
    def jsonSlurper = new JsonSlurper()
    def config = jsonSlurper.parseText(content)

    def acrCredential = new UsernamePasswordCredentialsImpl(
        CredentialsScope.GLOBAL,
        credentialId,
        'Azure Container Registry',
        config.aadClientId,
        config.aadClientSecret
    )
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), acrCredential)
}

void addAzureCredential(String credentialId, String configFile) {
    String content = new File(configFile).text
    def jsonSlurper = new JsonSlurper()
    def config = jsonSlurper.parseText(content)

    def azureCredential = new AzureCredentials(
        CredentialsScope.GLOBAL,
        credentialId,
        'Azure Service Principal',
        config.subscriptionId,
        config.aadClientId,
        config.aadClientSecret,
        'https://login.microsoftonline.com/' + config.tenantId + '/oauth2',
        '',
        '',
        '',
        ''
    )
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), azureCredential)
}

void addSSHCredential(String credentialId) {
    def credential = new BasicSSHUserPrivateKey(
        CredentialsScope.GLOBAL,
        credentialId,
        "azureuser",
        new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource('/root/.ssh/id_rsa'),
        '',
        ''
    )
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), credential)
}

/**
 * Configure Kubernetes plugin
 */
void configureKubernetes() {
    def instance = Jenkins.getInstance()
    def env = System.getenv()
    KubernetesCloud kube = new KubernetesCloud(
            'jenkins-slave',
            null,
            "https://${env.KUBERNETES_SERVICE_HOST}",
            "jenkins",
            "http://${env.JENKINS_SERVICE_HOST}",
            '5', 0, 0, 5)
    kube.setSkipTlsVerify(true)
    this.addKubeCredential('kube')
    kube.setCredentialsId('kube')

    def volumes = new ArrayList<PodVolume>()
    volumes.add(new HostPathVolume ('/etc/kubernetes', '/etc/kubernetes'))
    volumes.add(new HostPathVolume ('/var/run/docker.sock', '/var/run/docker.sock'))
    volumes.add(new SecretVolume ('/home/jenkins/.kube', 'kube-config'))

    def envVars = new ArrayList<PodEnvVar>()
    envVars.add(new PodEnvVar('GROUP_SUFFIX', "${env.GROUP_SUFFIX}"))

    def pod = new PodTemplate('jnlp', 'microsoft/java-on-azure-jenkins-slave:0.1', volumes)
    pod.setEnvVars(envVars)
    pod.setLabel('jenkins-slave-docker')
    pod.setRemoteFs('/home/jenkins')
    pod.setIdleMinutes(5)
    pod.setPrivileged(true)
    pod.setCommand('')
    pod.setArgs('')

    kube.addTemplate(pod)
    instance.clouds.replace(kube)
    instance.save()
}

Thread.start {
    def env = System.getenv()
    // Create or update pipelines
    String githubRepo = "${env.GITHUB_REPO_OWNER}/${env.GITHUB_REPO_NAME}"
    this.createPipeline(githubRepo, 'dev', 'master')
    this.createPipeline(githubRepo, 'test', 'test')
    this.createPipeline(githubRepo, 'prod', 'prod')
    // Configure Kubernetes plugin
    this.configureKubernetes()
    this.addACRCredential('acr', '/etc/kubernetes/azure.json')
    this.addAzureCredential('azure-sp', '/etc/kubernetes/azure.json')
    this.addSSHCredential('acs-ssh')
    // Set number of executor to 0 so that slave agents will be created for each build
    this.setExecutorNum(0)
    // Setup security
    this.setupSecurity()
    this.createUser('jenkins', "${env.JENKINS_PASSWORD}", 'jenkins')
}
