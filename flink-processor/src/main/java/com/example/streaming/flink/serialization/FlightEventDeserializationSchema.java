package com.example.streaming.flink.serialization;

import com.example.streaming.model.FlightEvent;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class FlightEventDeserializationSchema implements KafkaRecordDeserializationSchema<FlightEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<FlightEvent> out) throws IOException {
        FlightEvent event = objectMapper.readValue(record.value(), FlightEvent.class);
        out.collect(event);
    }

    @Override
    public TypeInformation<FlightEvent> getProducedType() {
        return TypeInformation.of(FlightEvent.class);
    }
}
