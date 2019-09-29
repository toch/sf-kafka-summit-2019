# SF Kafka Summit 2019: Cross The Streams thanks to Kafka and Flink

## Requirements

* minikube 1.2.0

## Installation of the cluster

```bash
# start a local k8s: minikube
minikube start --cpus 4 --memory 8192

# Init Helm
helm init --wait

# Add Confluent Helm repository
helm repo add confluentinc https://confluentinc.github.io/cp-helm-charts/
helm repo update

# Deploy Kafka and Co. cluster
helm install -f kafka-cluster-values.yaml confluentinc/cp-helm-charts --name my

# Deploy Flink cluster
kubectl create -f jobmanager-service.yaml
kubectl create -f jobmanager-deployment.yaml
kubectl create -f taskmanager-deployment.yaml

# Deploy PostgreSQL DB
helm install -f pgsql-values.yaml stable/postgresql

# Find PostgreSQL service
pgsql_svc=$(kubectl get svc --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}' | grep postgresql-headless)

# Expose it on local port 7000
kubectl port-forward svc/$pgsql_svc 7000:5432 &

# Set aside the PID of the port forwarder process
pgsql_port_fwd_pid=$!

# Seed
psql postgresql://postgres:password@localhost:7000/ghostbusters -f seed.sql

# Find Kafka Connect service
kafka_connect_svc=$(kubectl get svc --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}' | grep kafka-connect)

# Expose it on local port 7001
kubectl port-forward svc/$kafka_connect_svc 7001:8083 &

# Set aside the PID of the port forwarder process
kc_port_fwd_pid=$!

# Create the source connector
curl -X POST -H "Content-Type: application/json" --data "$(cat source-config.json | sed -e "s/DATABASE_HOSTNAME/$pgsql_svc/")" http://localhost:7001/connectors
```

## Flink Job

You can bootstrap a Maven project with

```bash
curl https://flink.apache.org/q/quickstart.sh | bash -s 1.9.0
``` 

To compile the project, do the following:

```bash
cd crossthestreams

# run the test
mvn test

# package
mvn clean package

cd ..

# Expose Flink JobManager on local port 7002
kubectl port-forward svc/flink-jobmanager 7002:8081 &

# Set aside the PID of the port forwarder process
fjm_port_fwd_pid=$!

# Upload Flink Job Jar
job_id=$(curl -X POST -H "Expect:" -F "jarfile=@crossthestreams/target/crossing_the_streams-1.0-SNAPSHOT.jar" http://localhost:7002/jars/upload | jq '.filename' | sed -e "s/.*\/\(.*_crossing_the_streams-1.0-SNAPSHOT.jar\)\"/\1/")

# Run it
curl -X POST http://localhost:7002/jars/$job_id/run?entry-class=conf.kafkasummit.talk.StreamCrossingJob

# Inspect the messages produced onto the resulting Kafka topic
kubectl exec -t my-cp-kafka-0 -c cp-kafka-broker -- kafka-console-consumer --from-beginning --bootstrap-server my-cp-kafka-headless:9092 --topic ghostbusters_ghost_appearances --max-messages 42

# Kill all port forwarder processes
kill $pgsql_port_fwd_pid $kc_port_fwd_pid $fjm_port_fwd_pid
```

## Others

* [Data source](https://ghostbusters.fandom.com/wiki/Ghostbusters_Wiki:Paranormal_Database)
* Ideas:
  * Helm chart repo for Flink
  * Add Schema Registry + Avro
