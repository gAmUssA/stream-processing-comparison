package com.example.streaming.flink;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import com.example.streaming.flink.serialization.FlightEventDeserializationSchema;
import com.example.streaming.flink.serialization.RouteDelayStatsSerializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class FlightDelayProcessor {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Enable checkpointing for fault tolerance
        env.enableCheckpointing(60000); // Checkpoint every minute
        
        // Configure Kafka source
        KafkaSource<FlightEvent> source = KafkaSource.<FlightEvent>builder()
                .setBootstrapServers("localhost:29092")
                .setTopics("flight-status")
                .setGroupId("flink-flight-processor")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(new FlightEventDeserializationSchema())
                .build();

        // Configure watermark strategy with event time
        WatermarkStrategy<FlightEvent> watermarkStrategy = WatermarkStrategy
                .<FlightEvent>forBoundedOutOfOrderness(Duration.ofMinutes(1))
                .withTimestampAssigner((event, timestamp) -> event.getEventTimestamp());

        // Create main data stream
        DataStream<FlightEvent> flightEvents = env
                .fromSource(source, watermarkStrategy, "Flight Events")
                .name("flight-events-source");

        // Process flight delays
        DataStream<RouteDelayStats> routeStats = flightEvents
                .keyBy(FlightEvent::getRouteKey)
                .window(TumblingEventTimeWindows.of(Time.minutes(15)))
                .process(new FlightDelayWindowProcessor())
                .name("route-delay-processor");

        // Split stream for different outputs
        DataStream<RouteDelayStats> alerts = routeStats
                .filter(RouteDelayStats::isHighRiskRoute)
                .name("high-risk-routes");

        // Configure Kafka sinks
        KafkaSink<RouteDelayStats> statsSink = KafkaSink.<RouteDelayStats>builder()
                .setBootstrapServers("localhost:29092")
                .setRecordSerializer(new RouteDelayStatsSerializer("route-stats"))
                .build();

        KafkaSink<RouteDelayStats> alertsSink = KafkaSink.<RouteDelayStats>builder()
                .setBootstrapServers("localhost:29092")
                .setRecordSerializer(new RouteDelayStatsSerializer("delay-alerts"))
                .build();

        // Write to Kafka
        routeStats.sinkTo(statsSink).name("route-stats-sink");
        alerts.sinkTo(alertsSink).name("alerts-sink");

        env.execute("Flight Delay Processor");
    }
}
