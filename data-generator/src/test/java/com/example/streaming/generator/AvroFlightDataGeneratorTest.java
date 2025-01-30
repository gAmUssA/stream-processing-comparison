package com.example.streaming.generator;

import com.example.streaming.model.Flight;

import net.christophschubert.cp.testcontainers.CPTestContainerFactory;
import net.christophschubert.cp.testcontainers.SchemaRegistryContainer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;

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
//@Testcontainers
class AvroFlightDataGeneratorTest {

  private CPTestContainerFactory testContainerFactory;
  private static KafkaContainer kafka;
  private static SchemaRegistryContainer schemaRegistry;

  @BeforeEach
  public void setUp() {
    // Initialize container factory
    testContainerFactory = new CPTestContainerFactory().withTag("7.8.0");

    // Create and start Kafka and Schema Registry containers
    kafka = testContainerFactory.createKafka();
    schemaRegistry = testContainerFactory.createSchemaRegistry(kafka);

    schemaRegistry.start(); // Starts both Schema Registry and Kafka containers
  }

  @AfterAll
  static void tearDown() {
    if (schemaRegistry != null) {
      schemaRegistry.stop();
    }
    kafka.stop();
  }

  @Test
  void testFlightDataGeneration() throws Exception {
    String bootstrapServers = kafka.getBootstrapServers();
    String schemaRegistryUrl = "http://localhost:" + schemaRegistry.getMappedPort(8081);

    // Client Props
    final String topicName = "flights-avro-stuff-test";

    // Create a topic
    final Properties props = createConsumerConfig(bootstrapServers, schemaRegistryUrl);
    AdminClient adminClient = AdminClient.create(props);
    adminClient.createTopics(Collections.singleton(new NewTopic(topicName, 2, (short) 1))).all().get();
    adminClient.close();

    // Start the generator
    AvroFlightDataGenerator generator = new AvroFlightDataGenerator(bootstrapServers, schemaRegistryUrl, topicName);
    int expectedMessages = 5;
    Thread generatorThread = new Thread(() -> generator.generateData(expectedMessages, 100));
    generatorThread.start();
    
    List<Flight> receivedFlights = new ArrayList<>();
    try (KafkaConsumer<String, Flight> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(Collections.singletonList(topicName));

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
      assertNotNull(flight.getFlightNumber(), "Flight number should not be null");
      assertNotNull(flight.getAirline(), "Airline should not be null");
      assertNotNull(flight.getOrigin(), "Origin should not be null");
      assertNotNull(flight.getDestination(), "Destination should not be null");
      assertNotEquals(flight.getOrigin(), flight.getDestination(),
                      "Origin and destination should be different");
      assertNotNull(flight.getScheduledDeparture(), "Scheduled departure should not be null");
      assertNotNull(flight.getStatus(), "Status should not be null");
      
      // Actual departure is optional
      if (flight.getActualDeparture() != null) {
        assertTrue(flight.getActualDeparture() >= flight.getScheduledDeparture(),
                   "Actual departure should not be before scheduled departure");
      }
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