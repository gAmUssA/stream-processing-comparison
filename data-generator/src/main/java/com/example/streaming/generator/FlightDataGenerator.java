package com.example.streaming.generator;

import com.example.streaming.model.FlightEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.datafaker.Faker;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FlightDataGenerator {

  private static final Logger logger = LoggerFactory.getLogger(FlightDataGenerator.class);
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  private final KafkaProducer<String, String> producer;
  private final Faker faker = new Faker();
  private final String topic;
  private final Map<String, FlightState> activeFlights = new ConcurrentHashMap<>();
  private final int generationIntervalMs;
  private final int numberOfFlights;

  // Major airports for more realistic routes
  private static final List<String> MAJOR_AIRPORTS = Arrays.asList(
      "JFK", "LAX", "ORD", "DFW", "SFO", "MIA", "ATL", "SEA"
  );

  // Major airlines for more realistic flight numbers
  private static final List<String> MAJOR_AIRLINES = Arrays.asList(
      "AA", "UA", "DL", "WN", "AS", "B6", "NK", "F9"
  );

  // Track flight states for realistic updates
  private static class FlightState {
    LocalDateTime scheduledDeparture;
    String departureAirport;
    String arrivalAirport;
    int delayMinutes;
    boolean isCompleted;
  }

  public FlightDataGenerator(Properties config) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getProperty("kafka.bootstrap.servers", "localhost:29092"));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, "1");
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    props.put(ProducerConfig.RETRIES_CONFIG, 3);

    this.producer = new KafkaProducer<>(props);
    this.topic = config.getProperty("kafka.topic", "flight-status");
    this.generationIntervalMs = Integer.parseInt(config.getProperty("generation.interval.ms", "5000"));
    this.numberOfFlights = Integer.parseInt(config.getProperty("number.of.flights", "10"));

    logger.info("Initialized FlightDataGenerator with bootstrap servers: {}, topic: {}, interval: {}ms, flights: {}",
        props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG), topic, generationIntervalMs, numberOfFlights);
  }

  public void startGenerating() {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    // Generate new flights based on configured interval
    executor.scheduleAtFixedRate(this::generateNewFlight, 0, generationIntervalMs, TimeUnit.MILLISECONDS);

    // Update existing flights every second
    executor.scheduleAtFixedRate(this::updateExistingFlights, 1, 1, TimeUnit.SECONDS);
  }

  private void generateNewFlight() {
    // Only generate new flights if we haven't reached the limit
    if (activeFlights.size() >= numberOfFlights) {
      logger.debug("Maximum number of active flights ({}) reached", numberOfFlights);
      return;
    }

    try {
      String departureAirport = MAJOR_AIRPORTS.get(faker.random().nextInt(MAJOR_AIRPORTS.size()));
      String arrivalAirport;
      do {
        arrivalAirport = MAJOR_AIRPORTS.get(faker.random().nextInt(MAJOR_AIRPORTS.size()));
      } while (arrivalAirport.equals(departureAirport));

      String airline = MAJOR_AIRLINES.get(faker.random().nextInt(MAJOR_AIRLINES.size()));
      String flightNumber = airline + faker.number().numberBetween(1000, 9999);

      MDC.put("flight", flightNumber);
      logger.info("Generating new flight {} from {} to {}", flightNumber, departureAirport, arrivalAirport);

      LocalDateTime scheduledDeparture = LocalDateTime.now().plusMinutes(faker.random().nextInt(60, 180));

      FlightEvent event = new FlightEvent(
          flightNumber,
          airline,
          departureAirport,
          arrivalAirport,
          scheduledDeparture,
          scheduledDeparture,
          "SCHEDULED"
      );

      sendEvent(event);

      FlightState state = new FlightState();
      state.scheduledDeparture = scheduledDeparture;
      state.departureAirport = departureAirport;
      state.arrivalAirport = arrivalAirport;
      state.delayMinutes = 0;
      state.isCompleted = false;

      activeFlights.put(flightNumber, state);
    } finally {
      MDC.remove("flight");
    }
  }

  private void updateExistingFlights() {
    LocalDateTime now = LocalDateTime.now();

    activeFlights.forEach((flightNumber, state) -> {
      try {
        MDC.put("flight", flightNumber);
        if (!state.isCompleted) {
          LocalDateTime scheduledDeparture = state.scheduledDeparture;

          // Randomly add delays as we approach departure time
          if (now.isAfter(scheduledDeparture.minusMinutes(30)) && state.delayMinutes == 0) {
            state.delayMinutes = faker.random().nextInt(0, 60);
            if (state.delayMinutes > 0) {
              logger.info("Flight {} delayed by {} minutes", flightNumber, state.delayMinutes);
            }
          }

          FlightEvent event = new FlightEvent(
              flightNumber,
              flightNumber.substring(0, 2),
              state.departureAirport,
              state.arrivalAirport,
              scheduledDeparture,
              state.delayMinutes > 0 ? scheduledDeparture.plusMinutes(state.delayMinutes) : scheduledDeparture,
              state.delayMinutes > 0 ? "DELAYED" : "ON_TIME"
          );

          sendEvent(event);

          // Mark flight as completed after departure
          if (now.isAfter(scheduledDeparture.plusMinutes(state.delayMinutes))) {
            state.isCompleted = true;
            logger.info("Flight {} completed", flightNumber);
          }
        }
      } finally {
        MDC.remove("flight");
      }
    });

    // Clean up completed flights older than 1 hour
    activeFlights.entrySet().removeIf(entry ->
        entry.getValue().isCompleted &&
        entry.getValue().scheduledDeparture.plusHours(1).isBefore(LocalDateTime.now())
    );
  }

  private void sendEvent(FlightEvent event) {
    try {
      String json = objectMapper.writeValueAsString(event);
      ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getFlightNumber(), json);
      producer.send(record, (metadata, exception) -> {
        if (exception != null) {
          logger.error("Error sending event for flight {}", event.getFlightNumber(), exception);
        } else {
          logger.debug("Event sent for flight {} to partition {} offset {}",
              event.getFlightNumber(), metadata.partition(), metadata.offset());
        }
      });
    } catch (Exception e) {
      logger.error("Error serializing event for flight {}", event.getFlightNumber(), e);
    }
  }

  public void shutdown() {
    producer.close();
  }

  public static void main(String[] args) {
    Properties config = new Properties();
    
    // Load from system properties
    config.putAll(System.getProperties());
    
    // Override with environment variables if present
    String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
    String topic = System.getenv("KAFKA_TOPIC");
    String intervalMs = System.getenv("GENERATION_INTERVAL_MS");
    String numFlights = System.getenv("NUMBER_OF_FLIGHTS");
    
    if (bootstrapServers != null) config.setProperty("kafka.bootstrap.servers", bootstrapServers);
    if (topic != null) config.setProperty("kafka.topic", topic);
    if (intervalMs != null) config.setProperty("generation.interval.ms", intervalMs);
    if (numFlights != null) config.setProperty("number.of.flights", numFlights);

    // Override with command line arguments if present
    if (args.length > 0) config.setProperty("kafka.bootstrap.servers", args[0]);
    if (args.length > 1) config.setProperty("kafka.topic", args[1]);
    if (args.length > 2) config.setProperty("generation.interval.ms", args[2]);
    if (args.length > 3) config.setProperty("number.of.flights", args[3]);

    FlightDataGenerator generator = new FlightDataGenerator(config);
    generator.startGenerating();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      generator.shutdown();
      System.exit(0);
    }));
  }
}
