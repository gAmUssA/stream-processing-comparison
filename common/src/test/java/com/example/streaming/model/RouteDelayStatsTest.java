package com.example.streaming.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RouteDelayStatsTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private RouteDelayStats stats;

    @BeforeEach
    void setUp() {
        stats = new RouteDelayStats();
        stats.setRouteKey("JFK-LAX");
    }

    @Test
    void shouldInitializeWithZeroValues() {
        assertEquals("JFK-LAX", stats.getRouteKey());
        assertEquals(0, stats.getCurrentWindowSize());
        assertEquals(0.0, stats.getAverageDelay());
        assertFalse(stats.isHighRiskRoute());
        assertTrue(stats.getDelays().isEmpty());
    }

    @Test
    void shouldAddFlightAndUpdateStats() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 11, 15, 40, 29);
        
        FlightEvent flight1 = new FlightEvent(
            "AA123",
            "AA",
            "JFK",
            "LAX",
            now,
            now.plusMinutes(30),
            "DELAYED"
        );
        
        stats.addFlight(flight1);
        
        assertEquals("JFK-LAX", stats.getRouteKey());
        assertEquals(1, stats.getCurrentWindowSize());
        assertEquals(30.0, stats.getAverageDelay());
        assertFalse(stats.isHighRiskRoute());
        assertEquals(1, stats.getDelays().size());
        assertEquals(30.0, stats.getDelays().get(0));
    }

    @Test
    void shouldIdentifyHighRiskRoute() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 11, 15, 40, 29);
        
        // Add 5 flights with 40-minute delays
        for (int i = 0; i < 5; i++) {
            FlightEvent flight = new FlightEvent(
                "AA" + i,
                "AA",
                "JFK",
                "LAX",
                now.plusMinutes(i * 5),
                now.plusMinutes(i * 5 + 40),
                "DELAYED"
            );
            stats.addFlight(flight);
        }
        
        assertEquals("JFK-LAX", stats.getRouteKey());
        assertEquals(5, stats.getCurrentWindowSize());
        assertEquals(40.0, stats.getAverageDelay());
        assertTrue(stats.isHighRiskRoute());
        assertEquals(5, stats.getDelays().size());
    }

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        LocalDateTime now = LocalDateTime.of(2025, 1, 11, 15, 40, 29);
        
        FlightEvent flight = new FlightEvent(
            "AA123",
            "AA",
            "JFK",
            "LAX",
            now,
            now.plusMinutes(30),
            "DELAYED"
        );
        
        stats.addFlight(flight);
        
        String json = objectMapper.writeValueAsString(stats);
        RouteDelayStats deserialized = objectMapper.readValue(json, RouteDelayStats.class);
        
        assertEquals(stats.getRouteKey(), deserialized.getRouteKey());
        assertEquals(stats.getCurrentWindowSize(), deserialized.getCurrentWindowSize());
        assertEquals(stats.getAverageDelay(), deserialized.getAverageDelay());
        assertEquals(stats.isHighRiskRoute(), deserialized.isHighRiskRoute());
        assertEquals(stats.getDelays(), deserialized.getDelays());
    }
}
