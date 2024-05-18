ARG VARIANT=21
FROM mcr.microsoft.com/vscode/devcontainers/java:${VARIANT}

# [Optional] Install Maven
ARG INSTALL_MAVEN="true"
ARG MAVEN_VERSION=3.6.3
RUN if [ "${INSTALL_MAVEN}" = "true" ]; then su vscode -c "source /usr/local/sdkman/bin/sdkman-init.sh && sdk install maven \"${MAVEN_VERSION}\""; fi

# [Optional] Install Gradle
ARG INSTALL_GRADLE="false"
ARG GRADLE_VERSION=6.7
RUN if [ "${INSTALL_GRADLE}" = "true" ]; then su vscode -c "source /usr/local/sdkman/bin/sdkman-init.sh && sdk install gradle \"${GRADLE_VERSION}\""; fi

# [Optional] Install your cloud CLI
# Options: "az", "gcloud", "aws", "kubectl", "doctl", "ibmcloud", "openfaas", "terraform", "cf", "oc", "none"
ARG INSTALL_AZURE_CLI="false"
RUN if [ "${INSTALL_AZURE_CLI}" = "true" ]; then su vscode -c ". /usr/local/etc/profile.d/az.sh && az version"; fi