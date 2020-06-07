#!/bin/bash

setupJava(){
    export JAVA_HOME=`type -p javac|xargs readlink -f|xargs dirname|xargs dirname|sed 's/8/11/'`
    export PATH=$PATH:$JAVA_HOME/bin/
    export M2_HOME=/usr/share/maven/
    export M2=$M2_HOME
    export MAVEN_OPTS='-Xmx640m -Xms256m'
    export PATH=$M2:$PATH
}

DIR=$(pwd)
if [[ $DIR == *"IoT_Sec_Gateway"* ]]; then
    DIR=$(echo $DIR | cut -d '/' -f -3)
fi
if [[ $DIR == *"l2switch"* ]]; then
    DIR=$(echo $DIR | cut -d '/' -f -3)
fi

setupJava

mvn clean install -s /home/pi/.m2/settings.xml -Pq -DskipTests -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true
