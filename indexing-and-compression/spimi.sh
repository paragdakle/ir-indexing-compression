#! /bin/bash

source /usr/local/corenlp350/classpath.sh
javac -d bin -cp $CLASSPATH src/main/java/filter/*.java src/main/java/io/*.java src/main/java/utils/*.java src/main/java/compression/*.java src/main/java/model/*.java src/main/java/nlp/*.java src/main/java/SPIMI.java
java -cp "bin:$CLASSPATH" main.java.SPIMI $1 $2
