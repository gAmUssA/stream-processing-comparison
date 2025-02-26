package com.example.streaming.flink.generator;

import com.example.streaming.flink.generator.serialization.FlightAvroSerializationSchema;
import com.example.streaming.model.Flight;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * A Flink job that generates flight data and writes it to a Kafka topic.
 * The job uses a custom source to generate the data and Confluent's Avro serializer
 * to integrate with Schema Registry.
 */
@Slf4j
public class DataGeneratorJob {

    private static final String DEFAULT_PROPERTIES_FILE = "producer.properties";

    public static void main(String[] args) throws Exception {
        // Parse command line parameters
        ParameterTool params = ParameterTool.fromArgs(args);
        String propertiesFile = params.get("properties", DEFAULT_PROPERTIES_FILE);

        // Load properties from file and override with environment variables
        Properties properties = loadProperties(propertiesFile);
        overrideWithEnvVars(properties);

        // Set up the execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Enable checkpointing for fault tolerance
        env.enableCheckpointing(60000); // Checkpoint every minute
        
        // Create the data generator source
        DataGeneratorSource source = new DataGeneratorSource(properties);
        
        // Define watermark strategy with event time
        WatermarkStrategy<Flight> watermarkStrategy = WatermarkStrategy
                .<Flight>forBoundedOutOfOrderness(Duration.ofMinutes(1))
                .withTimestampAssigner((event, timestamp) -> event.getScheduledDeparture());
        
        // Create a custom source function that uses our DataGeneratorSource
        SourceFunction<Flight> sourceFunction = new SourceFunction<Flight>() {
            private static final long serialVersionUID = 1L;
            private volatile boolean isRunning = true;
            private final int maxCount = Integer.parseInt(properties.getProperty("generator.count", "1000"));
            private final long delayBetweenRecords = 1000 / Integer.parseInt(properties.getProperty("generator.rate", "10"));
            
            @Override
            public void run(SourceContext<Flight> ctx) throws Exception {
                int count = 0;
                while (isRunning && count < maxCount) {
                    Flight flight = source.generateFlight();
                    ctx.collect(flight);
                    count++;
                    Thread.sleep(delayBetweenRecords);
                }
            }
            
            @Override
            public void cancel() {
                isRunning = false;
            }
        };
        
        // Create the data stream
        DataStream<Flight> flightStream = env
                .addSource(sourceFunction)
                .name("flight-data-generator")
                .assignTimestampsAndWatermarks(watermarkStrategy);
        
        // Configure Kafka sink
        String topic = properties.getProperty("topic.name", "flight-status-avro");
        String bootstrapServers = properties.getProperty("bootstrap.servers", "localhost:29092");
        
        KafkaSinkBuilder<Flight> builder = KafkaSink.<Flight>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(new FlightAvroSerializationSchema(topic, properties));
        
        // Use the correct DeliveryGuarantee
        KafkaSink<Flight> kafkaSink = builder
                .setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
        
        // Write to Kafka
        flightStream.sinkTo(kafkaSink).name("kafka-avro-sink");
        
        // Print job details
        log.info("Starting Flight Data Generator Job");
        log.info("Bootstrap Servers: {}", bootstrapServers);
        log.info("Topic: {}", topic);
        log.info("Schema Registry URL: {}", properties.getProperty("schema.registry.url"));
        
        // Execute the job
        env.execute("Flight Data Generator");
    }

    /**
     * Loads properties from a file.
     *
     * @param propertiesFile Path to the properties file
     * @return Properties loaded from the file
     * @throws IOException If the file cannot be read
     */
    private static Properties loadProperties(String propertiesFile) throws IOException {
        Properties properties = new Properties();
        
        try (InputStream inputStream = DataGeneratorJob.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            if (inputStream != null) {
                properties.load(inputStream);
                log.info("Loaded properties from classpath: {}", propertiesFile);
            } else {
                // Try to load from file system
                try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
                    properties.load(fileInputStream);
                    log.info("Loaded properties from file: {}", propertiesFile);
                }
            }
        } catch (IOException e) {
            log.warn("Could not load properties from {}: {}", propertiesFile, e.getMessage());
            log.info("Using default properties");
        }
        
        return properties;
    }

    /**
     * Overrides properties with environment variables.
     * Environment variables should be in the format KAFKA_BOOTSTRAP_SERVERS for bootstrap.servers.
     *
     * @param properties Properties to override
     */
    private static void overrideWithEnvVars(Properties properties) {
        // Override bootstrap.servers
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers != null) {
            properties.put("bootstrap.servers", bootstrapServers);
        }
        
        // Override schema.registry.url
        String schemaRegistryUrl = System.getenv("SCHEMA_REGISTRY_URL");
        if (schemaRegistryUrl != null) {
            properties.put("schema.registry.url", schemaRegistryUrl);
        }
        
        // Override topic.name
        String topicName = System.getenv("KAFKA_TOPIC");
        if (topicName != null) {
            properties.put("topic.name", topicName);
        }
        
        // Override generator.rate
        String generatorRate = System.getenv("GENERATOR_RATE");
        if (generatorRate != null) {
            properties.put("generator.rate", generatorRate);
        }
        
        // Override generator.count
        String generatorCount = System.getenv("GENERATOR_COUNT");
        if (generatorCount != null) {
            properties.put("generator.count", generatorCount);
        }
    }
}
