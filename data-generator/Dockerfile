FROM eclipse-temurin:25.0.1_8-jre

WORKDIR /app

COPY build/libs/data-generator-*.jar app.jar

ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    KAFKA_TOPIC=flight-status \
    GENERATION_INTERVAL_MS=1000 \
    NUMBER_OF_FLIGHTS=10 \
    SCHEMA_REGISTRY_URL=http://schema-registry:8081 \
    MESSAGE_COUNT=1000 \
    INTERVAL_MS=1000

ENTRYPOINT ["java", \
    "-Dkafka.bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS}", \
    "-Dkafka.topic=${KAFKA_TOPIC}", \
    "-Dgeneration.interval.ms=${GENERATION_INTERVAL_MS}", \
    "-Dnumber.of.flights=${NUMBER_OF_FLIGHTS}", \
    "-Dschema.registry.url=${SCHEMA_REGISTRY_URL}", \
    "-Dmessage.count=${MESSAGE_COUNT}", \
    "-Dinterval.ms=${INTERVAL_MS}", \
    "-jar", "app.jar"]
