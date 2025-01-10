package com.example.streaming.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/**
 * Represents a flight event in our system.
 * This class is designed to work seamlessly with both Kafka Streams and Flink serialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightEvent {
    private String flightNumber;
    private String airline;
    private String departureAirport;
    private String arrivalAirport;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledDeparture;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualDeparture;
    
    private String status;
    private long eventTimestamp;

    public FlightEvent(String flightNumber, String airline, String departureAirport, 
                      String arrivalAirport, LocalDateTime scheduledDeparture, 
                      LocalDateTime actualDeparture, String status) {
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.scheduledDeparture = scheduledDeparture;
        this.actualDeparture = actualDeparture;
        this.status = status;
        this.eventTimestamp = System.currentTimeMillis();
    }

    /**
     * Calculate delay in minutes between scheduled and actual departure times.
     * @return delay in minutes, or 0 if either time is null
     */
    @JsonIgnore
    public long getDelayMinutes() {
        if (actualDeparture == null || scheduledDeparture == null) {
            return 0;
        }
        return Duration.between(scheduledDeparture, actualDeparture).toMinutes();
    }

    /**
     * Creates a route key for aggregations in the format "departureAirport-arrivalAirport"
     * @return route key string
     */
    @JsonIgnore
    public String getRouteKey() {
        return departureAirport + "-" + arrivalAirport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightEvent that = (FlightEvent) o;
        return Objects.equals(flightNumber, that.flightNumber) &&
               Objects.equals(eventTimestamp, that.eventTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightNumber, eventTimestamp);
    }
}
