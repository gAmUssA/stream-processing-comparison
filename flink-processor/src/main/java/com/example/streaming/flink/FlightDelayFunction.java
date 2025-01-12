package com.example.streaming.flink;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class FlightDelayFunction extends KeyedProcessFunction<String, FlightEvent, RouteDelayStats> {
    private static final Logger logger = LoggerFactory.getLogger(FlightDelayFunction.class);
    private String operatorName;

    @Override
    public void open(Configuration parameters) {
        operatorName = getRuntimeContext().getTaskNameWithSubtasks();
        MDC.put("operator", operatorName);
        logger.info("Starting flight delay processor");
    }

    @Override
    public void close() {
        logger.info("Shutting down flight delay processor");
        MDC.remove("operator");
    }

    @Override
    public void processElement(
            FlightEvent event,
            Context context,
            Collector<RouteDelayStats> out) throws Exception {
        
        try {
            MDC.put("operator", operatorName);
            MDC.put("route", event.getRouteKey());
            
            logger.debug("Processing flight event: {}", event);
            
            // Your processing logic here
            // ...
            
        } finally {
            MDC.remove("route");
            MDC.remove("operator");
        }
    }
}
