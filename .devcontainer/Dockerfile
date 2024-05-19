# Set VARIANT to 21
ARG VARIANT=21

# Use mcr.microsoft.com/vscode/devcontainers/java:${VARIANT} as base image
FROM mcr.microsoft.com/vscode/devcontainers/java:${VARIANT}

USER root

RUN apt-get -y update && \
    apt-get -y install git wget sudo zip

# mvn
ARG MAVEN_VERSION=3.9.6
ARG BASE_URL=https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref && \
  curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
  tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 && \
  rm -f /tmp/apache-maven.tar.gz && \
  ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "/workspaces/vscode-remote-quarkus/.m2"

ADD settings.xml /usr/share/maven/conf/

# docker
RUN wget https://download.docker.com/linux/static/stable/x86_64/docker-20.10.2.tgz && \
    tar xzvf docker-20.10.2.tgz && \
    cp docker/* /usr/bin/

RUN bash -c "$(curl -fsSL https://raw.githubusercontent.com/ohmybash/oh-my-bash/master/tools/install.sh)"

# install quarkus cli
RUN curl -Ls https://sh.jbang.dev | bash -s - trust add https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/ && \
    curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --verbose --force quarkus@quarkusio

RUN echo 'shopt -s histappend' >> /root/.bashrc && \
    echo 'PROMPT_COMMAND=history -a;$PROMPT_COMMAND' >> /root/.bashrc && \
    sed -i 's/OSH_THEME="font"/OSH_THEME="powerline-plain"/g' /root/.bashrc && \
    echo 'HISTFILE=/workspaces/vscode-remote-quarkus/.bash_history' >> /root/.bashrc