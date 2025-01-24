package com.example.streaming.kafka;

import com.example.streaming.model.FlightEvent;
import com.example.streaming.model.RouteDelayStats;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class CustomSerdes {

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
      .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
      .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

  public static class FlightEventSerializer implements Serializer<FlightEvent> {

    @Override
    public byte[] serialize(String topic, FlightEvent data) {
      try {
        return data != null ? objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8) : null;
      } catch (Exception e) {
        throw new RuntimeException("Error serializing FlightEvent", e);
      }
    }
  }

  public static class FlightEventDeserializer implements Deserializer<FlightEvent> {

    @Override
    public FlightEvent deserialize(String topic, byte[] data) {
      try {
        return data != null ? objectMapper.readValue(data, FlightEvent.class) : null;
      } catch (Exception e) {
        throw new RuntimeException("Error deserializing FlightEvent", e);
      }
    }

  }

  public static class RouteDelayStatsSerializer implements Serializer<RouteDelayStats> {

    @Override
    public byte[] serialize(String topic, RouteDelayStats data) {
      try {
        return data != null ? objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8) : null;
      } catch (Exception e) {
        throw new RuntimeException("Error serializing RouteDelayStats", e);
      }
    }

  }

  public static class RouteDelayStatsDeserializer implements Deserializer<RouteDelayStats> {

    @Override
    public RouteDelayStats deserialize(String topic, byte[] data) {
      try {
        return data != null ? objectMapper.readValue(data, RouteDelayStats.class) : null;
      } catch (Exception e) {
        throw new RuntimeException("Error deserializing RouteDelayStats", e);
      }
    }

  }

  public static Serde<FlightEvent> flightEvent() {
    return Serdes.serdeFrom(new FlightEventSerializer(), new FlightEventDeserializer());
  }

  public static Serde<RouteDelayStats> routeDelayStats() {
    return Serdes.serdeFrom(new RouteDelayStatsSerializer(), new RouteDelayStatsDeserializer());
  }
}
