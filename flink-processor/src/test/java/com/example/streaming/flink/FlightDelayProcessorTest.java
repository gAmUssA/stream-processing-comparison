package com.example.streaming.flink;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightDelayProcessorTest {
    private static final Logger LOG = LoggerFactory.getLogger(FlightDelayProcessorTest.class);

    private StreamExecutionEnvironment env;

    @BeforeEach
    void setUp() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        
        // Configure Kryo serialization
        ExecutionConfig config = env.getConfig();
        config.enableForceKryo();
        config.registerTypeWithKryoSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    }

    @Test
    void testProcessElement() throws Exception {
        // Create test flight event
        FlightEvent event = createFlightEvent("NYC", "LAX", 15);
        List<FlightEvent> events = new ArrayList<>();
        events.add(event);

        // Create input stream
        DataStream<FlightEvent> input = env
            .fromCollection(events)
            .assignTimestampsAndWatermarks(
                new AscendingTimestampExtractor<FlightEvent>() {
                    @Override
                    public long extractAscendingTimestamp(FlightEvent event) {
                        return event.getEventTimestamp();
                    }
                }
            );

        // Process events
        DataStream<RouteDelayStats> output = input
            .keyBy(FlightEvent::getRouteKey)
            .window(TumblingEventTimeWindows.of(Time.minutes(15)))
            .process(new FlightDelayWindowProcessor());

        // Collect and verify results
        List<RouteDelayStats> results = output.executeAndCollect(1);
        assertFalse(results.isEmpty());

        RouteDelayStats stats = results.get(0);
        LOG.info("Stats for single event: {}", stats);
        assertNotNull(stats);
        assertEquals("NYC-LAX", stats.getRouteKey());
        assertEquals(15, stats.getAverageDelay(), 0.01);
        assertEquals(1, stats.getCurrentWindowSize());
    }

    @Test
    void testHighRiskRoute() throws Exception {
        // Create multiple delayed flights for the same route
        String from = "NYC";
        String to = "LAX";
        long baseTime = Instant.now().toEpochMilli();
        
        List<FlightEvent> events = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            FlightEvent event = createFlightEvent(from, to, 35); // Increase delay to ensure it's over 30 minutes
            LOG.info("Created event {}: {}", i, event);
            events.add(event);
        }

        // Create input stream
        DataStream<FlightEvent> input = env
            .fromCollection(events)
            .assignTimestampsAndWatermarks(
                new AscendingTimestampExtractor<FlightEvent>() {
                    @Override
                    public long extractAscendingTimestamp(FlightEvent event) {
                        return event.getEventTimestamp();
                    }
                }
            );

        // Process events
        DataStream<RouteDelayStats> output = input
            .keyBy(FlightEvent::getRouteKey)
            .window(TumblingEventTimeWindows.of(Time.minutes(15)))
            .process(new FlightDelayWindowProcessor());

        // Collect and verify results
        List<RouteDelayStats> results = output.executeAndCollect(1);
        assertFalse(results.isEmpty());

        RouteDelayStats stats = results.get(0);
        LOG.info("Stats for high risk route: {}", stats);
        assertNotNull(stats);
        assertTrue(stats.isHighRiskRoute(), "Route should be high risk with 5 flights and average delay > 30 minutes");
        assertEquals(35, stats.getAverageDelay(), 0.01);
        assertEquals(5, stats.getCurrentWindowSize());
    }

    private FlightEvent createFlightEvent(String from, String to, int delayMinutes) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime scheduled = now.minusMinutes(delayMinutes); // Remove the buffer to get exact delay
        LocalDateTime actual = now; // Set actual to now to get exact delay
        
        FlightEvent event = new FlightEvent(
            "FL123",
            "TestAir",
            from,
            to,
            scheduled,
            actual,
            "DEPARTED"
        );
        LOG.info("Created event with delay: {} minutes", event.getDelayMinutes());
        return event;
    }
}
