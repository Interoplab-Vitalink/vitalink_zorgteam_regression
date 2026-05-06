FROM jenkins/inbound-agent:alpine-jdk21

USER root

RUN apk add --no-cache bash git maven python3 py3-pip

USER jenkins

ENV MAVEN_OPTS="-Dmaven.repo.local=/home/jenkins/.m2/repository"
WORKDIR /home/jenkins/agent
