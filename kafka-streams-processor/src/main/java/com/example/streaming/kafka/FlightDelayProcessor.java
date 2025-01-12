package com.example.streaming.kafka;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;

public class FlightDelayProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FlightDelayProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
    private final String inputTopic;
    private final String delaysTopic;
    private final String alertsTopic;
    private static final String STORE_NAME = "route-stats-store";

    public FlightDelayProcessor(String inputTopic, String delaysTopic, String alertsTopic) {
        this.inputTopic = inputTopic;
        this.delaysTopic = delaysTopic;
        this.alertsTopic = alertsTopic;
    }

    public void start(Properties props) {
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "flight-delay-processor");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, CustomRocksDBConfig.class.getName());
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 0);
        
        Topology topology = buildTopology();
        KafkaStreams streams = new KafkaStreams(topology, props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }

    public Topology buildTopology() {
        logger.info("Building topology...");
        StreamsBuilder builder = new StreamsBuilder();

        // Create the stream from the input topic
        KStream<String, String> inputStream = builder.stream(inputTopic);

        // Process flight events and aggregate stats
        TimeWindows timeWindow = TimeWindows.ofSizeAndGrace(
            Duration.ofMinutes(15),
            Duration.ofMinutes(1)
        ).advanceBy(Duration.ofMinutes(1));
        
        // Convert input JSON to FlightEvent
        KStream<String, FlightEvent> flightStream = inputStream
            .mapValues((key, value) -> {
                if (value == null) {
                    logger.debug("Received null value");
                    return null;
                }
                try {
                    logger.debug("Parsing flight event: {}", value);
                    return objectMapper.readValue(value, FlightEvent.class);
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing flight event: {}", value, e);
                    return null;
                }
            })
            .filter((key, value) -> value != null && value.isValid())
            .selectKey((key, value) -> value.getRouteKey());

        // Aggregate flight delays into stats
        KTable<Windowed<String>, RouteDelayStats> statsTable = flightStream
            .groupByKey(Grouped.with(Serdes.String(), CustomSerdes.flightEvent()))
            .windowedBy(timeWindow)
            .aggregate(
                () -> {
                    RouteDelayStats stats = new RouteDelayStats();
                    stats.setCurrentWindowSize(0);
                    return stats;
                },
                (key, value, aggregate) -> {
                    logger.debug("Aggregating flight {} into stats for route {}", value, key);
                    aggregate.setRouteKey(key);
                    aggregate.addFlight(value);
                    return aggregate;
                },
                Materialized.<String, RouteDelayStats, WindowStore<Bytes, byte[]>>as(STORE_NAME)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(CustomSerdes.routeDelayStats())
                    .withRetention(Duration.ofHours(24))
                    .withLoggingEnabled(new HashMap<>())
                    .withCachingEnabled()
            );

        // Convert windowed KTable to KStream
        KStream<String, RouteDelayStats> statsStream = statsTable
            .toStream()
            .selectKey((windowed, value) -> windowed.key());

        // Split stream based on high risk status
        KStream<String, RouteDelayStats>[] branches = statsStream.branch(
            (key, stats) -> stats != null && stats.isHighRiskRoute() && stats.getCurrentWindowSize() >= 5,
            (key, stats) -> true
        );

        // Convert stats to JSON for output
        KStream<String, String> statsJsonStream = branches[1]
            .mapValues((key, stats) -> {
                try {
                    logger.debug("Converting stats to JSON: {}", stats);
                    return objectMapper.writeValueAsString(stats);
                } catch (JsonProcessingException e) {
                    logger.error("Error serializing stats: {}", stats, e);
                    return null;
                }
            })
            .filter((key, value) -> value != null);

        // Generate alerts for high-risk routes
        KStream<String, String> alertsStream = branches[0]
            .mapValues(stats -> {
                String alert = String.format(
                    "HIGH RISK ROUTE ALERT - %s has %d delays with average delay of %.1f minutes",
                    stats.getRouteKey(),
                    stats.getCurrentWindowSize(),
                    stats.getAverageDelay()
                );
                logger.info("Generated alert: {}", alert);
                return alert;
            });

        // Output to topics
        statsJsonStream.to(delaysTopic);
        alertsStream.to(alertsTopic);

        return builder.build();
    }
}
