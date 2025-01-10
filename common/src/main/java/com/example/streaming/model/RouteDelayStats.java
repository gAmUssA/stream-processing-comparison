package com.example.streaming.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Maintains statistics about delays for a specific route.
 * This class demonstrates stateful computations in both frameworks.
 */
@Data
@NoArgsConstructor
public class RouteDelayStats {
    private String routeId;
    private double averageDelay;
    private long totalFlights;
    private long delayedFlights;
    private long windowStartTime;
    private long windowEndTime;
    
    // Queue to maintain recent delays for rolling calculations
    @JsonIgnore // We don't need to serialize this for transmission
    private Queue<DelayRecord> recentDelays = new LinkedList<>();

    public RouteDelayStats(String routeId) {
        this.routeId = routeId;
        this.windowStartTime = System.currentTimeMillis();
        this.windowEndTime = this.windowStartTime;
        this.averageDelay = 0.0;
        this.totalFlights = 0;
        this.delayedFlights = 0;
    }

    /**
     * Add a new delay record and update statistics
     * @param delayMinutes the delay in minutes
     * @param timestamp the timestamp of the event
     */
    public void addDelay(long delayMinutes, long timestamp) {
        // Remove old entries (older than 15 minutes)
        long cutoffTime = timestamp - (15 * 60 * 1000);
        while (!recentDelays.isEmpty() && recentDelays.peek().timestamp < cutoffTime) {
            recentDelays.poll();
        }

        // Add new delay
        recentDelays.offer(new DelayRecord(delayMinutes, timestamp));
        
        // Update statistics
        this.totalFlights++;
        if (delayMinutes > 0) {
            this.delayedFlights++;
        }
        
        // Calculate new average
        this.averageDelay = recentDelays.stream()
            .mapToLong(record -> record.delayMinutes)
            .average()
            .orElse(0.0);
            
        this.windowEndTime = timestamp;
    }

    /**
     * Get the current number of delay records in the window
     * @return number of records
     */
    @JsonIgnore
    public int getCurrentWindowSize() {
        return recentDelays.size();
    }

    /**
     * Get the delay percentage (delayed flights / total flights)
     * @return percentage of delayed flights
     */
    @JsonIgnore
    public double getDelayPercentage() {
        return totalFlights > 0 ? (double) delayedFlights / totalFlights * 100 : 0.0;
    }

    /**
     * Determines if this route is experiencing significant delays
     * that might affect connected flights.
     * @return true if the route is considered high risk
     */
    @JsonIgnore
    public boolean isHighRiskRoute() {
        return averageDelay > 15 && // More than 15 minutes average delay
               delayedFlights >= 3;  // At least 3 delayed flights
    }

    /**
     * Inner class to track individual delay records
     */
    private static class DelayRecord {
        final long delayMinutes;
        final long timestamp;

        DelayRecord(long delayMinutes, long timestamp) {
            this.delayMinutes = delayMinutes;
            this.timestamp = timestamp;
        }
    }
}
