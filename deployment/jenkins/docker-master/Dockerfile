FROM jenkins/jenkins:lts

USER root

ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

# Copy scripts to Jenkins image
COPY scriptApproval.xml /usr/share/jenkins/ref/
COPY init.groovy /usr/share/jenkins/ref/

# Install suggested plugins
RUN /usr/local/bin/install-plugins.sh \
    cloudbees-folder \
    antisamy-markup-formatter \
    build-timeout \
    credentials-binding \
    timestamper \
    ws-cleanup \
    ant \
    gradle \
    workflow-job \
    workflow-multibranch \
    workflow-aggregator \
    github-organization-folder \
    pipeline-stage-view \
    git \
    subversion \
    ssh-slaves \
    matrix-auth \
    pam-auth \
    ldap \
    email-ext \
    mailer \
    kubernetes \
    job-dsl \
    groovy \
    azure-app-service \
    azure-acs \
    azure-function
