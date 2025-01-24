package com.example.streaming.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Represents a flight event with status information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightEvent {
    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime actualDeparture;
    private String status;

    @JsonCreator
    public FlightEvent(
        @JsonProperty("flightNumber") String flightNumber,
        @JsonProperty("airline") String airline,
        @JsonProperty("origin") String origin,
        @JsonProperty("destination") String destination,
        @JsonProperty("scheduledDeparture") LocalDateTime scheduledDeparture,
        @JsonProperty("actualDeparture") LocalDateTime actualDeparture,
        @JsonProperty("status") String status
    ) {
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.origin = origin;
        this.destination = destination;
        this.scheduledDeparture = scheduledDeparture;
        this.actualDeparture = actualDeparture;
        this.status = status;
    }

    public FlightEvent() {
        // Default constructor for Jackson
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(LocalDateTime scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public LocalDateTime getActualDeparture() {
        return actualDeparture;
    }

    public void setActualDeparture(LocalDateTime actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isValid() {
        return flightNumber != null && !flightNumber.isEmpty() &&
               airline != null && !airline.isEmpty() &&
               origin != null && !origin.isEmpty() &&
               destination != null && !destination.isEmpty() &&
               scheduledDeparture != null &&
               status != null && !status.isEmpty();
    }

    public String getRouteKey() {
        return origin + "-" + destination;
    }

    public double getDelayMinutes() {
        if (scheduledDeparture == null || actualDeparture == null) {
            return 0.0;
        }
        return ChronoUnit.MINUTES.between(scheduledDeparture, actualDeparture);
    }

    public long getEventTimestamp() {
        if (actualDeparture != null) {
            return actualDeparture.toInstant(ZoneOffset.UTC).toEpochMilli();
        } else if (scheduledDeparture != null) {
            return scheduledDeparture.toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return System.currentTimeMillis(); // Default to current time if no timestamps available
    }

    @Override
    public String toString() {
        return String.format(
            "FlightEvent{flightNumber='%s', airline='%s', origin='%s', destination='%s', scheduled='%s', actual='%s', status='%s'}",
            flightNumber, airline, origin, destination, scheduledDeparture, actualDeparture, status
        );
    }
}
