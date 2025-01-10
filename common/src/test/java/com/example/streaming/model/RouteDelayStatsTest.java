package com.example.streaming.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteDelayStatsTest {
    private RouteDelayStats stats;
    private ObjectMapper objectMapper;
    private static final String ROUTE_ID = "JFK-LAX";
    private static final long BASE_TIME = 1705000000000L; // 2024-01-11T19:33:20Z

    @BeforeEach
    void setUp() {
        stats = new RouteDelayStats(ROUTE_ID);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testInitialState() {
        assertEquals(ROUTE_ID, stats.getRouteId());
        assertEquals(0.0, stats.getAverageDelay());
        assertEquals(0, stats.getTotalFlights());
        assertEquals(0, stats.getDelayedFlights());
        assertEquals(0, stats.getCurrentWindowSize());
        assertEquals(0.0, stats.getDelayPercentage());
        assertFalse(stats.isHighRiskRoute());
    }

    @Test
    void testAddDelay() {
        // Add some delays
        stats.addDelay(20, BASE_TIME);
        stats.addDelay(40, BASE_TIME + 1000); // 1 second later
        stats.addDelay(30, BASE_TIME + 2000); // 2 seconds later

        assertEquals(3, stats.getTotalFlights());
        assertEquals(3, stats.getDelayedFlights());
        assertEquals(30.0, stats.getAverageDelay());
        assertEquals(3, stats.getCurrentWindowSize());
        assertEquals(100.0, stats.getDelayPercentage());
        assertTrue(stats.isHighRiskRoute());
    }

    @Test
    void testWindowExpiry() {
        // Add delays with timestamps spanning more than 15 minutes
        stats.addDelay(20, BASE_TIME); // This should be removed
        stats.addDelay(40, BASE_TIME + (14 * 60 * 1000)); // 14 minutes later
        stats.addDelay(30, BASE_TIME + (16 * 60 * 1000)); // 16 minutes later

        // Only the last two records should remain
        assertEquals(3, stats.getTotalFlights()); // Total flights count is cumulative
        assertEquals(2, stats.getCurrentWindowSize()); // But window size reflects current window
        assertEquals(35.0, stats.getAverageDelay()); // Average of remaining delays
    }

    @Test
    void testZeroDelays() {
        stats.addDelay(0, BASE_TIME);
        stats.addDelay(0, BASE_TIME + 1000);

        assertEquals(2, stats.getTotalFlights());
        assertEquals(0, stats.getDelayedFlights());
        assertEquals(0.0, stats.getAverageDelay());
        assertEquals(0.0, stats.getDelayPercentage());
        assertFalse(stats.isHighRiskRoute());
    }

    @Test
    void testHighRiskRoute() {
        // Add delays that should make it high risk
        stats.addDelay(35, BASE_TIME);
        stats.addDelay(40, BASE_TIME + 1000);
        stats.addDelay(45, BASE_TIME + 2000);
        stats.addDelay(50, BASE_TIME + 3000);

        assertTrue(stats.isHighRiskRoute());
        assertEquals(42.5, stats.getAverageDelay());
        assertEquals(4, stats.getDelayedFlights());
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Add some data
        stats.addDelay(20, BASE_TIME);
        stats.addDelay(40, BASE_TIME + 1000);

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(stats);
        
        // Deserialize and verify
        RouteDelayStats deserialized = objectMapper.readValue(json, RouteDelayStats.class);
        
        assertEquals(stats.getRouteId(), deserialized.getRouteId());
        assertEquals(stats.getAverageDelay(), deserialized.getAverageDelay());
        assertEquals(stats.getTotalFlights(), deserialized.getTotalFlights());
        assertEquals(stats.getDelayedFlights(), deserialized.getDelayedFlights());
        assertEquals(stats.getWindowStartTime(), deserialized.getWindowStartTime());
        assertEquals(stats.getWindowEndTime(), deserialized.getWindowEndTime());
        
        // Verify that recentDelays is not serialized
        assertFalse(json.contains("recentDelays"));
    }
}
