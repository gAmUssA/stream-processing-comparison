package com.example.streaming.flink.generator;

import com.example.streaming.model.Flight;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataGeneratorSourceTest {

    @Test
    void testGenerateFlights() {
        // Set up properties for testing
        Properties props = new Properties();
        props.setProperty("generator.count", "5");  // Generate only 5 records for testing
        props.setProperty("generator.rate", "100"); // Fast generation for testing
        
        // Create the source
        DataGeneratorSource source = new DataGeneratorSource(props);
        
        // Generate and validate 5 flights
        for (int i = 0; i < 5; i++) {
            Flight flight = source.generateFlight();
            assertNotNull(flight);
            assertNotNull(flight.getFlightNumber());
            assertNotNull(flight.getAirline());
            assertNotNull(flight.getOrigin());
            assertNotNull(flight.getDestination());
            assertNotNull(flight.getStatus());
        }
    }
}
