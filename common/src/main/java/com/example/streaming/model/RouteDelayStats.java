package com.example.streaming.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains statistics about delays for a specific route.
 * This class demonstrates stateful computations in both frameworks.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteDelayStats {
    private String routeKey;
    private List<Double> delays;
    private int currentWindowSize;
    private double averageDelay;
    private boolean highRiskRoute;

    @JsonCreator
    public RouteDelayStats(
        @JsonProperty("routeKey") String routeKey,
        @JsonProperty("delays") List<Double> delays,
        @JsonProperty("currentWindowSize") int currentWindowSize,
        @JsonProperty("averageDelay") double averageDelay,
        @JsonProperty("highRiskRoute") boolean highRiskRoute
    ) {
        this.routeKey = routeKey;
        this.delays = delays != null ? delays : new ArrayList<>();
        this.currentWindowSize = currentWindowSize;
        this.averageDelay = averageDelay;
        this.highRiskRoute = highRiskRoute;
    }

    public RouteDelayStats() {
        this(null, new ArrayList<>(), 0, 0.0, false);
    }

    public void addFlight(FlightEvent event) {
        if (routeKey == null) {
            routeKey = event.getRouteKey();
            delays = new ArrayList<>();
            currentWindowSize = 0;
            averageDelay = 0.0;
            highRiskRoute = false;
        }

        // Get delay in minutes from the event
        double delayMinutes = event.getDelayMinutes();
        delays.add(delayMinutes);
        currentWindowSize++;

        // Recalculate average delay
        averageDelay = delays.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        // Update high risk status
        // A route is considered high risk if:
        // 1. There are at least 5 flights in the window
        // 2. The average delay is more than 30 minutes
        highRiskRoute = currentWindowSize >= 5 && averageDelay > 30.0;
    }

    // Getters and setters
    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
        if (this.routeKey != null && !this.routeKey.equals(routeKey)) {
            // Reset state for new route
            delays = new ArrayList<>();
            currentWindowSize = 0;
            averageDelay = 0.0;
            highRiskRoute = false;
        }
    }

    public List<Double> getDelays() {
        return delays;
    }

    public void setDelays(List<Double> delays) {
        this.delays = delays != null ? delays : new ArrayList<>();
    }

    public int getCurrentWindowSize() {
        return currentWindowSize;
    }

    public void setCurrentWindowSize(int currentWindowSize) {
        this.currentWindowSize = currentWindowSize;
    }

    public double getAverageDelay() {
        return averageDelay;
    }

    public void setAverageDelay(double averageDelay) {
        this.averageDelay = averageDelay;
    }

    public boolean isHighRiskRoute() {
        return highRiskRoute;
    }

    public void setHighRiskRoute(boolean highRiskRoute) {
        this.highRiskRoute = highRiskRoute;
    }

    @Override
    public String toString() {
        return String.format(
            "RouteDelayStats{routeKey='%s', currentWindowSize=%d, averageDelay=%.1f, highRiskRoute=%s, delays=%s}",
            routeKey, currentWindowSize, averageDelay, highRiskRoute, delays
        );
    }
}
