#!/bin/bash
# Maven Wrapper for Windows/Linux

# Detect Maven
if command -v mvn &> /dev/null; then
    MVN_CMD=mvn
elif [ -f "$HOME/apache-maven-3.9.6/bin/mvn" ]; then
    MVN_CMD="$HOME/apache-maven-3.9.6/bin/mvn"
elif [ -f "/c/Program Files/apache-maven-3.9.6/bin/mvn" ]; then
    MVN_CMD="/c/Program Files/apache-maven-3.9.6/bin/mvn"
else
    echo "Maven not found. Please install Maven or add it to PATH."
    echo "Download: https://maven.apache.org/download.cgi"
    exit 1
fi

# Run Maven command
$MVN_CMD "$@"
