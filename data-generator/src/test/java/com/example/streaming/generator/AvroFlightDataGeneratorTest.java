package com.example.streaming.generator;

import com.example.streaming.model.Flight;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class AvroFlightDataGeneratorTest {

  private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.5.1";
  private static final String SCHEMA_REGISTRY_IMAGE = "confluentinc/cp-schema-registry:7.5.1";
  private static Network network;
  private static ConfluentKafkaContainer kafka;
  private static GenericContainer<?> schemaRegistry;

  @BeforeAll
  static void setUp() {
    network = Network.newNetwork();

    // Start Kafka
    kafka = new ConfluentKafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
        .withNetwork(network)
        .withNetworkAliases("kafka");
    kafka.start();

    // Start Schema Registry
    schemaRegistry = new GenericContainer<>(DockerImageName.parse(SCHEMA_REGISTRY_IMAGE))
        .withNetwork(network)
        .withNetworkAliases("schema-registry")
        .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                 "PLAINTEXT://kafka:9092")
        .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
        .withExposedPorts(8081);
    schemaRegistry.start();
  }

  @AfterAll
  static void tearDown() {
    schemaRegistry.stop();
    kafka.stop();
    network.close();
  }

  @Test
  void testFlightDataGeneration() throws Exception {
    String bootstrapServers = kafka.getBootstrapServers();
    String schemaRegistryUrl = "http://localhost:" + schemaRegistry.getMappedPort(8081);

    // Start the generator
    AvroFlightDataGenerator generator = new AvroFlightDataGenerator(bootstrapServers, schemaRegistryUrl);
    int expectedMessages = 5;
    Thread generatorThread = new Thread(() -> generator.generateData(expectedMessages, 100));
    generatorThread.start();

    // Configure consumer
    final Properties props = createConsumerConfig(bootstrapServers, schemaRegistryUrl);

    List<Flight> receivedFlights = new ArrayList<>();
    try (KafkaConsumer<String, Flight> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(Collections.singletonList("flights"));

      int retries = 10;
      while (receivedFlights.size() < expectedMessages && retries > 0) {
        ConsumerRecords<String, Flight> records = consumer.poll(Duration.ofSeconds(1));
        for (ConsumerRecord<String, Flight> record : records) {
          receivedFlights.add(record.value());
        }
        retries--;
      }
    }

    generatorThread.join(10000);

    assertEquals(expectedMessages, receivedFlights.size(), "Should receive expected number of messages");

    // Validate flight data
    for (Flight flight : receivedFlights) {
      assertNotNull(flight.getFlightId(), "Flight ID should not be null");
      assertNotNull(flight.getFlightNumber(), "Flight number should not be null");
      assertNotNull(flight.getDepartureAirport(), "Departure airport should not be null");
      assertNotNull(flight.getArrivalAirport(), "Arrival airport should not be null");
      assertNotEquals(flight.getDepartureAirport(), flight.getArrivalAirport(),
                      "Departure and arrival airports should be different");
      assertTrue(flight.getDepartureTime() < flight.getArrivalTime(),
                 "Departure time should be before arrival time");
      assertNotNull(flight.getAircraft(), "Aircraft details should not be null");
      assertNotNull(flight.getAircraft().getModel(), "Aircraft model should not be null");
      assertNotNull(flight.getAircraft().getRegistration(), "Aircraft registration should not be null");
    }
  }

  private static @NotNull Properties createConsumerConfig(final String bootstrapServers, final String schemaRegistryUrl) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
    props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
    return props;
  }
}
