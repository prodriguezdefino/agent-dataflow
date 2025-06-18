source ./local_stop.sh

mvn spotless:apply clean package

java -jar agent/target/agent-1.0-SNAPSHOT.jar &
java -jar pipeline-tools/target/pipeline-tools-1.0-SNAPSHOT.jar &
