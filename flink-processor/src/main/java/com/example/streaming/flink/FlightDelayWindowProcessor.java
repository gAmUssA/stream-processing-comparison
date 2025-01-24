package com.example.streaming.flink;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FlightDelayWindowProcessor 
        extends ProcessWindowFunction<FlightEvent, RouteDelayStats, String, TimeWindow> {
    
    private static final Logger LOG = LoggerFactory.getLogger(FlightDelayWindowProcessor.class);

    @Override
    public void process(
            String key,
            Context context,
            Iterable<FlightEvent> events,
            Collector<RouteDelayStats> out) {
        
        RouteDelayStats stats = new RouteDelayStats();
        stats.setRouteKey(key);
        
        List<Double> delays = new ArrayList<>();
        long latestTimestamp = 0;
        
        LOG.info("Processing events for route key: {}", key);
        for (FlightEvent event : events) {
            LOG.info("Processing event: {}", event);
            double delay = event.getDelayMinutes();
            delays.add(delay);
            latestTimestamp = Math.max(latestTimestamp, event.getEventTimestamp());
        }
        
        if (!delays.isEmpty()) {
            stats.setDelays(delays);
            stats.setCurrentWindowSize(delays.size());
            double avgDelay = delays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            stats.setAverageDelay(avgDelay);
            stats.setLastUpdateTimestamp(latestTimestamp);
            boolean isHighRisk = delays.size() >= 5 && avgDelay > 30.0;
            stats.setHighRiskRoute(isHighRisk);
            LOG.info("Generated stats: {}", stats);
            out.collect(stats);
        }
    }
}
