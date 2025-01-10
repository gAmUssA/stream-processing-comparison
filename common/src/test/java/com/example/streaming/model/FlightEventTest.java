package com.example.streaming.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightEventTest {
    private FlightEvent flightEvent;
    private ObjectMapper objectMapper;
    private static final LocalDateTime SCHEDULED = LocalDateTime.of(2025, 1, 10, 10, 0);
    private static final LocalDateTime ACTUAL = LocalDateTime.of(2025, 1, 10, 10, 30);

    @BeforeEach
    void setUp() {
        flightEvent = new FlightEvent(
            "FL123",
            "TestAir",
            "JFK",
            "LAX",
            SCHEDULED,
            ACTUAL,
            "DELAYED"
        );
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testDelayMinutesCalculation() {
        assertEquals(30, flightEvent.getDelayMinutes(),
            "Delay should be 30 minutes");
    }

    @Test
    void testDelayMinutesWithNullTimes() {
        FlightEvent nullEvent = new FlightEvent();
        nullEvent.setScheduledDeparture(null);
        nullEvent.setActualDeparture(null);
        
        assertEquals(0, nullEvent.getDelayMinutes(),
            "Delay should be 0 when times are null");
    }

    @Test
    void testRouteKeyGeneration() {
        assertEquals("JFK-LAX", flightEvent.getRouteKey(),
            "Route key should be in format 'departure-arrival'");
    }

    @Test
    void testEqualsAndHashCode() {
        FlightEvent event1 = new FlightEvent("FL123", "TestAir", "JFK", "LAX", SCHEDULED, ACTUAL, "DELAYED");
        FlightEvent event2 = new FlightEvent("FL123", "TestAir", "JFK", "LAX", SCHEDULED, ACTUAL, "DELAYED");
        FlightEvent differentEvent = new FlightEvent("FL456", "TestAir", "JFK", "LAX", SCHEDULED, ACTUAL, "DELAYED");

        assertTrue(event1.equals(event2), "Same flight events should be equal");
        assertFalse(event1.equals(differentEvent), "Different flight events should not be equal");
        assertEquals(event1.hashCode(), event2.hashCode(), "Hash codes should be equal for equal events");
    }

    @Test
    void testJsonSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(flightEvent);
        FlightEvent deserializedEvent = objectMapper.readValue(json, FlightEvent.class);
        
        assertEquals(flightEvent.getFlightNumber(), deserializedEvent.getFlightNumber());
        assertEquals(flightEvent.getAirline(), deserializedEvent.getAirline());
        assertEquals(flightEvent.getDepartureAirport(), deserializedEvent.getDepartureAirport());
        assertEquals(flightEvent.getArrivalAirport(), deserializedEvent.getArrivalAirport());
        assertEquals(flightEvent.getScheduledDeparture(), deserializedEvent.getScheduledDeparture());
        assertEquals(flightEvent.getActualDeparture(), deserializedEvent.getActualDeparture());
        assertEquals(flightEvent.getStatus(), deserializedEvent.getStatus());
    }
}
