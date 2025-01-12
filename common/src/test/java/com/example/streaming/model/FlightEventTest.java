package com.example.streaming.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightEventTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void shouldCalculateRouteKey() {
        FlightEvent event = new FlightEvent(
            "AA123",
            "AA",
            "JFK",
            "LAX",
            LocalDateTime.now(),
            LocalDateTime.now(),
            "DELAYED"
        );
        assertEquals("JFK-LAX", event.getRouteKey());
    }

    @Test
    void shouldCalculateDelayMinutes() {
        LocalDateTime scheduled = LocalDateTime.of(2025, 1, 11, 15, 40, 29);
        LocalDateTime actual = scheduled.plusMinutes(30);
        
        FlightEvent event = new FlightEvent(
            "AA123",
            "AA",
            "JFK",
            "LAX",
            scheduled,
            actual,
            "DELAYED"
        );
        
        assertEquals(30, event.getDelayMinutes());
    }

    @Test
    void shouldHandleNullDepartureTimes() {
        FlightEvent event = new FlightEvent(
            "AA123",
            "AA",
            "JFK",
            "LAX",
            null,
            null,
            "SCHEDULED"
        );
        assertEquals(0, event.getDelayMinutes());
    }

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        LocalDateTime now = LocalDateTime.of(2025, 1, 11, 15, 40, 29);
        FlightEvent event = new FlightEvent(
            "AA123",
            "AA",
            "JFK",
            "LAX",
            now,
            now.plusMinutes(30),
            "DELAYED"
        );
        
        String json = objectMapper.writeValueAsString(event);
        FlightEvent deserialized = objectMapper.readValue(json, FlightEvent.class);
        
        assertEquals(event.getFlightNumber(), deserialized.getFlightNumber());
        assertEquals(event.getAirline(), deserialized.getAirline());
        assertEquals(event.getDepartureAirport(), deserialized.getDepartureAirport());
        assertEquals(event.getArrivalAirport(), deserialized.getArrivalAirport());
        assertEquals(event.getScheduledDepartureTime(), deserialized.getScheduledDepartureTime());
        assertEquals(event.getActualDepartureTime(), deserialized.getActualDepartureTime());
        assertEquals(event.getStatus(), deserialized.getStatus());
        assertEquals(event.getRouteKey(), deserialized.getRouteKey());
        assertEquals(event.getDelayMinutes(), deserialized.getDelayMinutes());
    }
}
