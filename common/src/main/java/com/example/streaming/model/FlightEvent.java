package com.example.streaming.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Represents a flight event in our system.
 * This class is designed to work seamlessly with both Kafka Streams and Flink serialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightEvent {
    private String flightNumber;
    private String airline;
    private String departureAirport;
    private String arrivalAirport;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime scheduledDepartureTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime actualDepartureTime;
    
    private String status;

    public FlightEvent() {
        // Default constructor for Jackson
    }

    public FlightEvent(String flightNumber, String airline, String departureAirport, String arrivalAirport,
                      LocalDateTime scheduledDepartureTime, LocalDateTime actualDepartureTime, String status) {
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.actualDepartureTime = actualDepartureTime;
        this.status = status;
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

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public LocalDateTime getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    @JsonSetter
    public void setScheduledDepartureTime(LocalDateTime scheduledDepartureTime) {
        this.scheduledDepartureTime = scheduledDepartureTime;
    }

    public void setScheduledDepartureTime(Instant scheduledDepartureTime) {
        this.scheduledDepartureTime = LocalDateTime.ofInstant(scheduledDepartureTime, ZoneOffset.UTC);
    }

    public LocalDateTime getActualDepartureTime() {
        return actualDepartureTime;
    }

    @JsonSetter
    public void setActualDepartureTime(LocalDateTime actualDepartureTime) {
        this.actualDepartureTime = actualDepartureTime;
    }

    public void setActualDepartureTime(Instant actualDepartureTime) {
        this.actualDepartureTime = LocalDateTime.ofInstant(actualDepartureTime, ZoneOffset.UTC);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getDelayMinutes() {
        if (scheduledDepartureTime == null || actualDepartureTime == null) {
            return 0.0;
        }
        return Duration.between(scheduledDepartureTime, actualDepartureTime).toMinutes();
    }

    public String getRouteKey() {
        return departureAirport + "-" + arrivalAirport;
    }

    public boolean isValid() {
        return flightNumber != null && !flightNumber.isEmpty() &&
               airline != null && !airline.isEmpty() &&
               departureAirport != null && !departureAirport.isEmpty() &&
               arrivalAirport != null && !arrivalAirport.isEmpty() &&
               scheduledDepartureTime != null &&
               actualDepartureTime != null &&
               status != null && !status.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightEvent that = (FlightEvent) o;
        return Objects.equals(flightNumber, that.flightNumber) &&
               Objects.equals(airline, that.airline) &&
               Objects.equals(departureAirport, that.departureAirport) &&
               Objects.equals(arrivalAirport, that.arrivalAirport) &&
               Objects.equals(scheduledDepartureTime, that.scheduledDepartureTime) &&
               Objects.equals(actualDepartureTime, that.actualDepartureTime) &&
               Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightNumber, airline, departureAirport, arrivalAirport,
                          scheduledDepartureTime, actualDepartureTime, status);
    }

    @Override
    public String toString() {
        return "FlightEvent{" +
               "flightNumber='" + flightNumber + '\'' +
               ", airline='" + airline + '\'' +
               ", departureAirport='" + departureAirport + '\'' +
               ", arrivalAirport='" + arrivalAirport + '\'' +
               ", scheduledDepartureTime=" + scheduledDepartureTime +
               ", actualDepartureTime=" + actualDepartureTime +
               ", status='" + status + '\'' +
               '}';
    }
}
