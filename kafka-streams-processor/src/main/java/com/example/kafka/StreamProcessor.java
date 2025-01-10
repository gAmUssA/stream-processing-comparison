package com.example.kafka;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;

public class StreamProcessor {
    public static Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        // Topology will be implemented later
        return builder.build();
    }
}
