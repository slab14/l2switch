#!/bin/bash

export JAVA_HOME=`type -p javac|xargs readlink -f|xargs dirname|xargs dirname`
export PATH=$PATH:$JAVA_HOME/bin/
export M2_HOME=/usr/share/maven/
export M2=$M2_HOME
export MAVEN_OPTS='-Xmx1048m -XX:MaxPermSize=512m -Xms256m'
export PATH=$M2:$PATH
