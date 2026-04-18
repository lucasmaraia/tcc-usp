#!/bin/sh
# Maven Wrapper startup script

MAVEN_PROJECTBASEDIR="${BASH_SOURCE[0]%/*}"
[ -z "$MAVEN_PROJECTBASEDIR" ] && MAVEN_PROJECTBASEDIR="$( cd "${BASH_SOURCE[0]%/*}" && pwd )"

[ -f "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" ] || {
    echo "Downloading Maven Wrapper..."
    WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    curl -fsSL -o "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" "$WRAPPER_URL"
}

exec java -jar "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" \
    -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" "$@"
