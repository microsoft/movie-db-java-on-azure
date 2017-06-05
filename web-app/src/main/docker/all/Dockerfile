FROM centos:7

RUN yum install -y java-1.8.0-openjdk.x86_64
ARG OVEROPSSK=""

# Takipi installation
RUN curl -Ls /dev/null http://get.takipi.com/takipi-t4c-installer | \
    bash /dev/stdin -i --sk=${OVEROPSSK}

VOLUME /tmp
ADD web-app-0.1.0-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app.jar'
ADD newrelic.jar newrelic.jar
RUN sh -c 'touch /newrelic.jar'
ADD newrelic.yml newrelic.yml
RUN sh -c 'touch /newrelic.yml'
ARG NEW_RELIC_APP_NAME=""
ENV NEW_RELIC_APP_NAME=${NEW_RELIC_APP_NAME}
ARG NEW_RELIC_LICENSE_KEY="" 
ENV NEW_RELIC_LICENSE_KEY=${NEW_RELIC_LICENSE_KEY}

# Connecting the Takipi agent to a Java process
CMD java -javaagent:/newrelic.jar -agentlib:TakipiAgent -jar /app.jar
