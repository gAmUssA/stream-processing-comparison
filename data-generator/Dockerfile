FROM eclipse-temurin:21.0.5_11-jre

WORKDIR /app

COPY build/libs/data-generator-*.jar app.jar

ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    KAFKA_TOPIC=flight-status \
    GENERATION_INTERVAL_MS=1000 \
    NUMBER_OF_FLIGHTS=10

ENTRYPOINT ["java", \
    "-Dkafka.bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS}", \
    "-Dkafka.topic=${KAFKA_TOPIC}", \
    "-Dgeneration.interval.ms=${GENERATION_INTERVAL_MS}", \
    "-Dnumber.of.flights=${NUMBER_OF_FLIGHTS}", \
    "-jar", "app.jar"]
