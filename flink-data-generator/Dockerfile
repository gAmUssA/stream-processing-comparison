FROM eclipse-temurin:25.0.1_8-jre-alpine

WORKDIR /app

# Copy the application JAR
COPY build/libs/flink-data-generator-*-all.jar /app/flink-data-generator.jar

# Copy the configuration files
COPY src/main/resources/producer.properties /app/producer.properties
COPY src/main/resources/logback.xml /app/logback.xml

# Set environment variables
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV SCHEMA_REGISTRY_URL=http://schema-registry:8081
ENV KAFKA_TOPIC=flight-status-avro
ENV GENERATOR_RATE=10
ENV GENERATOR_COUNT=1000

# Run the application
ENTRYPOINT ["java", "-Dlogback.configurationFile=/app/logback.xml", "-jar", "/app/flink-data-generator.jar", "--properties", "/app/producer.properties"]
