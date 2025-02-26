package com.example.streaming.flink.generator.serialization;

import com.example.streaming.model.Flight;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A serialization schema for Flight records that uses Confluent's KafkaAvroSerializer
 * to integrate with Schema Registry.
 */
@Slf4j
public class FlightAvroSerializationSchema implements KafkaRecordSerializationSchema<Flight> {

    private static final long serialVersionUID = 1L;

    private final String topic;
    private final String schemaRegistryUrl;
    private transient KafkaAvroSerializer serializer;
    private transient SchemaRegistryClient schemaRegistryClient;

    /**
     * Creates a new FlightAvroSerializationSchema with the specified topic and properties.
     *
     * @param topic The Kafka topic to write to
     * @param properties Properties containing Schema Registry configuration
     */
    public FlightAvroSerializationSchema(String topic, Properties properties) {
        this.topic = topic;
        this.schemaRegistryUrl = properties.getProperty("schema.registry.url", "http://localhost:8081");
    }

    @Nullable
    @Override
    public ProducerRecord<byte[], byte[]> serialize(Flight flight, KafkaSinkContext context, Long timestamp) {
        if (serializer == null) {
            initializeSerializer();
        }

        try {
            // Use the flight number as the key
            byte[] key = flight.getFlightNumber().getBytes();
            byte[] value = serializer.serialize(topic, flight);
            
            return new ProducerRecord<>(topic, null, timestamp, key, value);
        } catch (Exception e) {
            log.error("Error serializing Flight record", e);
            return null;
        }
    }

    private void initializeSerializer() {
        try {
            // Create a Schema Registry client
            schemaRegistryClient = new CachedSchemaRegistryClient(schemaRegistryUrl, 100);
            
            // Create and configure the serializer
            serializer = new KafkaAvroSerializer(schemaRegistryClient);
            Map<String, Object> configs = new HashMap<>();
            configs.put("schema.registry.url", schemaRegistryUrl);
            serializer.configure(configs, false);
            
            log.info("Initialized Avro serializer with Schema Registry at {}", schemaRegistryUrl);
        } catch (Exception e) {
            log.error("Failed to initialize Avro serializer", e);
            throw new RuntimeException("Could not initialize Avro serializer", e);
        }
    }
}
