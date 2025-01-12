package com.example.streaming.generator;

import com.example.streaming.model.FlightEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class FlightDataGeneratorTest {

  private static final String TOPIC = "test-flight-status";
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  @Container
  private static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

  private FlightDataGenerator generator;
  private KafkaConsumer<String, String> consumer;

  @BeforeEach
  void setUp() {
    Properties config = new Properties();
    config.setProperty("kafka.bootstrap.servers", kafka.getBootstrapServers());
    config.setProperty("kafka.topic", TOPIC);
    config.setProperty("generation.interval.ms", "1000");
    config.setProperty("number.of.flights", "5");

    generator = new FlightDataGenerator(config);

    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(Collections.singletonList(TOPIC));
  }

  @AfterEach
  void tearDown() {
    if (generator != null) {
      generator.shutdown();
    }
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  void testGeneratesValidFlightEvents() throws Exception {
    // Start generating events
    generator.startGenerating();

    // Wait and collect events
    AtomicInteger eventCount = new AtomicInteger(0);
    long startTime = System.currentTimeMillis();
    long timeout = 10000; // 10 seconds

    while (eventCount.get() < 5 && System.currentTimeMillis() - startTime < timeout) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
      for (ConsumerRecord<String, String> record : records) {
        String json = record.value();
        FlightEvent event = objectMapper.readValue(json, FlightEvent.class);

        // Verify event fields
        assertNotNull(event.getFlightNumber(), "Flight number should not be null");
        assertNotNull(event.getAirline(), "Airline should not be null");
        assertNotNull(event.getDepartureAirport(), "Departure airport should not be null");
        assertNotNull(event.getArrivalAirport(), "Arrival airport should not be null");
        assertNotNull(event.getScheduledDepartureTime(), "Scheduled departure time should not be null");
        assertNotNull(event.getActualDepartureTime(), "Actual departure time should not be null");
        assertNotNull(event.getStatus(), "Status should not be null");

        // Verify route key matches airports
        assertEquals(event.getDepartureAirport() + "-" + event.getArrivalAirport(),
            event.getRouteKey(), "Route key should match departure and arrival airports");

        // Verify departure and arrival airports are different
        assertNotEquals(event.getDepartureAirport(), event.getArrivalAirport(),
            "Departure and arrival airports should be different");

        eventCount.incrementAndGet();
      }
    }

    assertTrue(eventCount.get() > 0, "Should have received at least one event");
  }
}
