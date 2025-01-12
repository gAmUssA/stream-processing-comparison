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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FlightDataGeneratorTest {
    private static final String TOPIC = "test-flight-status";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private FlightDataGenerator generator;
    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        generator = new FlightDataGenerator(kafka.getBootstrapServers(), TOPIC);

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
        generator.shutdown();
        consumer.close();
    }

    @Test
    void testGeneratesValidFlightEvents() throws Exception {
        generator.startGenerating();

        AtomicInteger eventCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds timeout

        while (eventCount.get() < 3 && System.currentTimeMillis() - startTime < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                eventCount.incrementAndGet();
                
                // Verify the event can be deserialized
                FlightEvent event = objectMapper.readValue(record.value(), FlightEvent.class);
                
                // Basic validation
                assertNotNull(event.getFlightNumber());
                assertNotNull(event.getAirline());
                assertNotNull(event.getDepartureAirport());
                assertNotNull(event.getArrivalAirport());
                assertNotNull(event.getScheduledDepartureTime());
                assertNotNull(event.getActualDepartureTime());
                assertNotNull(event.getStatus());
                
                // Flight number should be the key
                assertEquals(event.getFlightNumber(), record.key());
                
                // Airports should be different
                assertNotEquals(event.getDepartureAirport(), event.getArrivalAirport());
            }
        }

        assertTrue(eventCount.get() > 0, "Should have received at least one event");
    }
}
