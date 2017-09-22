FROM jenkinsci/jnlp-slave:3.7-1

USER root

# Install apt-utils
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        apt-utils \
        curl

# Install maven 3.3.9
ENV MAVEN_VERSION 3.3.9
RUN curl -fsSL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
    && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
ENV MAVEN_HOME /usr/share/maven
ADD maven-settings.xml $MAVEN_HOME/conf/settings.xml

# Install docker
RUN apt-get install -y \
    apt-transport-https \
    ca-certificates \
    software-properties-common
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
RUN add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/debian \
   $(lsb_release -cs) \
   stable"
RUN apt-get update && \
    apt-get install -y docker-ce

# Install kubectl
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl

# Install azure cli
RUN echo "deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ wheezy main" | tee /etc/apt/sources.list.d/azure-cli.list
RUN apt-key adv --keyserver packages.microsoft.com --recv-keys 417A0893
RUN apt-get install apt-transport-https
RUN apt-get update && \
    apt-get install -y azure-cli

# Install envsubst
RUN apt-get install -y gettext
