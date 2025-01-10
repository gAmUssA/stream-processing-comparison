package com.example.streaming.generator;

import com.example.streaming.model.FlightEvent;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.*;
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

    public FlightDataGenerator(String bootstrapServers, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
    }

    public void startGenerating() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        
        // Generate new flights every 5 seconds
        executor.scheduleAtFixedRate(this::generateNewFlight, 0, 5, TimeUnit.SECONDS);
        
        // Update existing flights every second
        executor.scheduleAtFixedRate(this::updateExistingFlights, 1, 1, TimeUnit.SECONDS);
    }

    private void generateNewFlight() {
        try {
            String departureAirport = MAJOR_AIRPORTS.get(faker.random().nextInt(MAJOR_AIRPORTS.size()));
            String arrivalAirport;
            do {
                arrivalAirport = MAJOR_AIRPORTS.get(faker.random().nextInt(MAJOR_AIRPORTS.size()));
            } while (arrivalAirport.equals(departureAirport));

            String airline = MAJOR_AIRLINES.get(faker.random().nextInt(MAJOR_AIRLINES.size()));
            String flightNumber = airline + faker.number().numberBetween(1000, 9999);

            LocalDateTime scheduledDeparture = LocalDateTime.now().plusMinutes(faker.random().nextInt(60, 180));
            
            FlightEvent event = new FlightEvent(
                flightNumber,
                airline,
                departureAirport,
                arrivalAirport,
                scheduledDeparture,
                scheduledDeparture, // Initially actual = scheduled
                "SCHEDULED"
            );

            // Store flight state
            FlightState state = new FlightState();
            state.scheduledDeparture = scheduledDeparture;
            state.departureAirport = departureAirport;
            state.arrivalAirport = arrivalAirport;
            state.delayMinutes = 0;
            state.isCompleted = false;
            activeFlights.put(flightNumber, state);

            sendEvent(event);
            
        } catch (Exception e) {
            logger.error("Error generating flight", e);
        }
    }

    private void updateExistingFlights() {
        activeFlights.forEach((flightNumber, state) -> {
            try {
                // Simulate random delays
                if (!state.isCompleted && faker.random().nextInt(100) < 20) { // 20% chance of delay
                    state.delayMinutes += faker.random().nextInt(5, 15);

                    final FlightEvent event = getFlightEvent(flightNumber, state);

                    sendEvent(event);
                }
                
                // Complete flights that are past their time
                if (!state.isCompleted && 
                    state.scheduledDeparture.plusMinutes(state.delayMinutes).isBefore(LocalDateTime.now())) {
                    state.isCompleted = true;
                    activeFlights.remove(flightNumber);
                }
                
            } catch (Exception e) {
              logger.error("Error updating flight {}", flightNumber, e);
            }
        });
    }

    private static FlightEvent getFlightEvent(final String flightNumber, final FlightState state) {
        LocalDateTime actualDeparture = state.scheduledDeparture.plusMinutes(state.delayMinutes);

        FlightEvent event = new FlightEvent(
            flightNumber,
            flightNumber.substring(0, 2),
            state.departureAirport,
            state.arrivalAirport,
            state.scheduledDeparture,
            actualDeparture,
            state.delayMinutes > 30 ? "DELAYED" : "ON_TIME"
        );
        return event;
    }

    private void sendEvent(FlightEvent event) throws Exception {
        String json = objectMapper.writeValueAsString(event);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getFlightNumber(), json);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.error("Error sending event", exception);
            }
        });
    }

    public void shutdown() {
        producer.close();
    }

    public static void main(String[] args) {
        String bootstrapServers = args.length > 0 ? args[0] : "localhost:29092";
        String topic = args.length > 1 ? args[1] : "flight-status";
        
        FlightDataGenerator generator = new FlightDataGenerator(bootstrapServers, topic);
        generator.startGenerating();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            generator.shutdown();
            System.exit(0);
        }));
    }
}
