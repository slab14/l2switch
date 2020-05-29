#!/bin/bash

setupJava(){
    export JAVA_HOME=`type -p javac|xargs readlink -f|xargs dirname|xargs dirname|sed 's/8/11/'`
    export PATH=$PATH:$JAVA_HOME/bin/
    export M2_HOME=/usr/share/maven/
    export M2=$M2_HOME
    export MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=512m -Xms256m'
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

# for some reason, maven is not downloading the jar for org.apache.karaf.jaas.boot:4.1.7, so we can manually download this.
addFiles(){
    FILE="$DIR/l2switch/distribution/karaf/target/assembly/system/org/apache/karaf/jaas/org.apache.karaf.jaas.boot/4.1.7/org.apache.karaf.jaas.boot-4.1.7.jar"
    # first check if we have already installed it.
    if [[ ! -f "$FILE" ]]; then
	mkdir -p $DIR/l2switch/distribution/karaf/target/assembly/system/org/apache/karaf/jaas/org.apache.karaf.jaas.boot/4.1.7/
	touch $FILE
	wget -q -O - https://repo1.maven.org/maven2/org/apache/karaf/jaas/org.apache.karaf.jaas.boot/4.1.7/org.apache.karaf.jaas.boot-4.1.7.jar > $DIR/l2switch/distribution/karaf/target/assembly/system/org/apache/karaf/jaas/org.apache.karaf.jaas.boot/4.1.7/org.apache.karaf.jaas.boot-4.1.7.jar
    fi
}

export JAVA_MAX_MEM=512M
export JAVA_MAX_PERM_MEM=448M

# start ODL
cd $DIR/l2switch/distribution/karaf/target/assembly
#sudo -E ./bin/karaf
./bin/karaf 
