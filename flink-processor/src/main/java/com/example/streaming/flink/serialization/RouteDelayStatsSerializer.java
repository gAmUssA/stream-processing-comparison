package com.example.streaming.flink.serialization;

import com.example.streaming.model.RouteDelayStats;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RouteDelayStatsSerializer implements KafkaRecordSerializationSchema<RouteDelayStats> {
    private final String topic;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public RouteDelayStatsSerializer(String topic) {
        this.topic = topic;
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(RouteDelayStats stats, KafkaSinkContext context, Long timestamp) {
        try {
            byte[] value = objectMapper.writeValueAsBytes(stats);
            return new ProducerRecord<>(topic, stats.getRouteKey().getBytes(), value);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing RouteDelayStats", e);
        }
    }
}
