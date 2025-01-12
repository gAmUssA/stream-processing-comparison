package com.example.streaming.kafka;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class FlightDelayProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger(FlightDelayProcessorTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
    private static final String INPUT_TOPIC = "flight-status";
    private static final String DELAYS_TOPIC = "flight-delays";
    private static final String ALERTS_TOPIC = "flight-alerts";
    private static final String STORE_NAME = "route-stats-store";

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> delaysTopic;
    private TestOutputTopic<String, String> alertsTopic;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams-test");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        //props.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, CustomRocksDBConfig.class.getName());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 0);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        props.put(StreamsConfig.WINDOW_STORE_CHANGE_LOG_ADDITIONAL_RETENTION_MS_CONFIG, Duration.ofHours(1).toMillis());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 0);
        props.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);

        FlightDelayProcessor processor = new FlightDelayProcessor(INPUT_TOPIC, DELAYS_TOPIC, ALERTS_TOPIC);
        Topology topology = processor.buildTopology();
        System.out.println(topology.describe().toString());
        testDriver = new TopologyTestDriver(topology, props);

        inputTopic = testDriver.createInputTopic(
            INPUT_TOPIC,
            new StringSerializer(),
            new StringSerializer()
        );

        delaysTopic = testDriver.createOutputTopic(
            DELAYS_TOPIC,
            new StringDeserializer(),
            new StringDeserializer()
        );

        alertsTopic = testDriver.createOutputTopic(
            ALERTS_TOPIC,
            new StringDeserializer(),
            new StringDeserializer()
        );
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void shouldProcessFlightEventsAndGenerateStats() throws Exception {
        // Arrange
        Instant now = Instant.now();
        String routeKey = "JFK-LAX";
        
        FlightEvent flight1 = new FlightEvent();
        flight1.setFlightNumber("AA123");
        flight1.setAirline("AA");
        flight1.setDepartureAirport("JFK");
        flight1.setArrivalAirport("LAX");
        flight1.setScheduledDepartureTime(now);
        flight1.setActualDepartureTime(now.plusSeconds(1800)); // 30 min delay
        flight1.setStatus("DELAYED");
        
        logger.info("Flight1 before serialization: {}", flight1);
        logger.info("Flight1 scheduledDepartureTime: {}", flight1.getScheduledDepartureTime());
        logger.info("Flight1 actualDepartureTime: {}", flight1.getActualDepartureTime());

        FlightEvent flight2 = new FlightEvent();
        flight2.setFlightNumber("AA456");
        flight2.setAirline("AA");
        flight2.setDepartureAirport("JFK");
        flight2.setArrivalAirport("LAX");
        flight2.setScheduledDepartureTime(now.plusSeconds(300)); // 5 minutes later
        flight2.setActualDepartureTime(now.plusSeconds(1500)); // 20 min delay
        flight2.setStatus("DELAYED");

        logger.info("Flight2 before serialization: {}", flight2);
        logger.info("Flight2 scheduledDepartureTime: {}", flight2.getScheduledDepartureTime());
        logger.info("Flight2 actualDepartureTime: {}", flight2.getActualDepartureTime());

        // Act
        String flight1Json = objectMapper.writeValueAsString(flight1);
        logger.info("Flight1 JSON: {}", flight1Json);
        inputTopic.pipeInput(flight1.getFlightNumber(), flight1Json);
        testDriver.advanceWallClockTime(Duration.ofMinutes(1));
        
        String flight2Json = objectMapper.writeValueAsString(flight2);
        logger.info("Flight2 JSON: {}", flight2Json);
        inputTopic.pipeInput(flight2.getFlightNumber(), flight2Json);
        testDriver.advanceWallClockTime(Duration.ofMinutes(1));

        // Assert
        WindowStore<String, RouteDelayStats> store = testDriver.getWindowStore(STORE_NAME);
        assertNotNull(store, "Store should be available");
        
        RouteDelayStats stats = null;
        try (KeyValueIterator<Windowed<String>, RouteDelayStats> iterator = store.all()) {
            while (iterator.hasNext()) {
                KeyValue<Windowed<String>, RouteDelayStats> next = iterator.next();
                if (next.key.key().equals(routeKey)) {
                    stats = next.value;
                    break;
                }
            }
        }

        assertNotNull(stats, "Stats should be present in store");
        assertEquals(2, stats.getCurrentWindowSize(), "Should have both flights in window");
        assertEquals(25.0, stats.getAverageDelay(), 0.1, "Average delay should be 25 minutes");
        assertEquals(routeKey, stats.getRouteKey(), "Route key should match");
    }

    @Test
    @Disabled
    void shouldGenerateAlertForHighRiskRoute() throws Exception {
        // Arrange
        Instant now = Instant.now();
        String routeKey = "JFK-LAX";
        
        // Create 5 flights with delays over 30 minutes
        for (int i = 0; i < 5; i++) {
            FlightEvent flight = new FlightEvent();
            flight.setFlightNumber("AA" + i);
            flight.setAirline("AA");
            flight.setDepartureAirport("JFK");
            flight.setArrivalAirport("LAX");
            flight.setScheduledDepartureTime(now.plusSeconds(i * 300)); // 5 minutes apart
            flight.setActualDepartureTime(now.plusSeconds((i * 300) + 3600)); // 60 min delay each
            flight.setStatus("DELAYED");
            
            String flightJson = objectMapper.writeValueAsString(flight);
            logger.info("Flight {} JSON: {}", i, flightJson);
            inputTopic.pipeInput(flight.getFlightNumber(), flightJson);
            testDriver.advanceWallClockTime(Duration.ofMinutes(1));
        }

        // Assert that an alert is generated after 5 flights
        assertTrue(alertsTopic.isEmpty(), "Should not have generated an alert yet");
        
        // Add one more flight
        FlightEvent flight = new FlightEvent();
        flight.setFlightNumber("AA5");
        flight.setAirline("AA");
        flight.setDepartureAirport("JFK");
        flight.setArrivalAirport("LAX");
        flight.setScheduledDepartureTime(now.plusSeconds(1500)); // 25 minutes later
        flight.setActualDepartureTime(now.plusSeconds(5100)); // 60 min delay
        flight.setStatus("DELAYED");
        
        String flightJson = objectMapper.writeValueAsString(flight);
        logger.info("Final flight JSON: {}", flightJson);
        inputTopic.pipeInput(flight.getFlightNumber(), flightJson);
        testDriver.advanceWallClockTime(Duration.ofMinutes(1));

        // Should have another alert with 6 flights
        assertFalse(alertsTopic.isEmpty(), "Should have generated an alert with 6 flights");
        String alert = alertsTopic.readValue();
        assertTrue(alert.contains("HIGH RISK ROUTE"), "Alert should indicate high risk");
        assertTrue(alert.contains(routeKey), "Alert should contain route key");
        assertTrue(alert.contains("60.0"), "Alert should show correct average delay");
    }

    @Test
    void shouldMaintainStateInStore() throws Exception {
        // Arrange
        Instant now = Instant.now();
        String routeKey = "JFK-LAX";
        
        FlightEvent flight = new FlightEvent();
        flight.setFlightNumber("AA123");
        flight.setAirline("AA");
        flight.setDepartureAirport("JFK");
        flight.setArrivalAirport("LAX");
        flight.setScheduledDepartureTime(now);
        flight.setActualDepartureTime(now.plusSeconds(1800)); // 30 min delay
        flight.setStatus("DELAYED");

        // Act
        String flightJson = objectMapper.writeValueAsString(flight);
        logger.info("Flight JSON: {}", flightJson);
        inputTopic.pipeInput(flight.getFlightNumber(), flightJson); // Use flight number as the key
        testDriver.advanceWallClockTime(Duration.ofMinutes(1));

        // Assert
        WindowStore<String, RouteDelayStats> store = testDriver.getWindowStore(STORE_NAME);
        assertNotNull(store, "Store should be available");
        
        RouteDelayStats stats = null;
        try (KeyValueIterator<Windowed<String>, RouteDelayStats> iterator = store.all()) {
            while (iterator.hasNext()) {
                KeyValue<Windowed<String>, RouteDelayStats> next = iterator.next();
                if (next.key.key().equals(routeKey)) {
                    stats = next.value;
                    break;
                }
            }
        }

        assertNotNull(stats, "Stats should be present in store");
        assertEquals(1, stats.getCurrentWindowSize(), "Should have one flight in window");
        assertEquals(30.0, stats.getAverageDelay(), 0.1, "Average delay should be 30 minutes");
        assertEquals(routeKey, stats.getRouteKey(), "Route key should match");
    }

    @Test
    void shouldHandleNullAndInvalidInput() throws Exception {
        // Arrange
        FlightEvent invalidFlight = new FlightEvent(); // Missing required fields
        String invalidFlightJson = objectMapper.writeValueAsString(invalidFlight);
        
        // Act & Assert - flight with missing fields
        inputTopic.pipeInput("invalid-flight", invalidFlightJson);
        assertTrue(delaysTopic.isEmpty(), "Should not process flight with missing fields");
        assertTrue(alertsTopic.isEmpty(), "Should not generate alerts for invalid flight");

        // Act & Assert - invalid JSON
        inputTopic.pipeInput("invalid-flight", "invalid json");
        assertTrue(delaysTopic.isEmpty(), "Should not process invalid JSON");
        assertTrue(alertsTopic.isEmpty(), "Should not generate alerts for invalid JSON");
    }
}
