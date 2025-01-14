package com.example.streaming.generator;

import com.example.streaming.model.Aircraft;
import com.example.streaming.model.Flight;
import com.example.streaming.model.FlightStatus;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AvroFlightDataGenerator {
    private static final String[] AIRPORTS = {"JFK", "LAX", "ORD", "DFW", "DEN", "SFO", "SEA", "LAS", "MCO", "EWR"};
    private static final String[] AIRCRAFT_MODELS = {"Boeing 737", "Airbus A320", "Boeing 787", "Airbus A350", "Embraer E190"};
    private static final String DEFAULT_TOPIC = "flight-status-avro";
    
    private final KafkaProducer<String, Flight> producer;
    private final Faker faker;
    private final Random random;
    private final String topic;

    public AvroFlightDataGenerator(String bootstrapServers, String schemaRegistryUrl) {
        this(bootstrapServers, schemaRegistryUrl, DEFAULT_TOPIC);
    }

    public AvroFlightDataGenerator(String bootstrapServers, String schemaRegistryUrl, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);

        this.producer = new KafkaProducer<>(props);
        this.faker = new Faker();
        this.random = new Random();
        this.topic = topic;
    }

    private Flight generateFlight() {
        String flightId = UUID.randomUUID().toString();
        String flightNumber = faker.aviation().flight();
        
        String depAirport = AIRPORTS[random.nextInt(AIRPORTS.length)];
        String arrAirport;
        do {
            arrAirport = AIRPORTS[random.nextInt(AIRPORTS.length)];
        } while (arrAirport.equals(depAirport));

        Instant now = Instant.now();
        Instant departureTime = now.plus(random.nextInt(48), ChronoUnit.HOURS);
        Instant arrivalTime = departureTime.plus(2 + random.nextInt(8), ChronoUnit.HOURS);

        String aircraftModel = AIRCRAFT_MODELS[random.nextInt(AIRCRAFT_MODELS.length)];
        String registration = faker.aviation().aircraft();

        Aircraft aircraft = Aircraft.newBuilder()
                .setModel(aircraftModel)
                .setRegistration(registration)
                .build();

        FlightStatus status = random.nextInt(100) < 80 ? 
                FlightStatus.SCHEDULED : 
                FlightStatus.values()[random.nextInt(FlightStatus.values().length)];

        Integer delayMinutes = status == FlightStatus.DELAYED ? 
                random.nextInt(180) + 15 : null;

        return Flight.newBuilder()
                .setFlightId(flightId)
                .setFlightNumber(flightNumber)
                .setDepartureAirport(depAirport)
                .setArrivalAirport(arrAirport)
                .setDepartureTime(departureTime.toEpochMilli())
                .setArrivalTime(arrivalTime.toEpochMilli())
                .setStatus(status)
                .setAircraft(aircraft)
                .setDelayMinutes(delayMinutes)
                .setUpdateTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    public void generateData(int messageCount, long intervalMs) {
        try {
            for (int i = 0; i < messageCount; i++) {
                Flight flight = generateFlight();
                ProducerRecord<String, Flight> record = 
                    new ProducerRecord<>(topic, flight.getFlightId(), flight);

                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error sending message: ", exception);
                    } else {
                        log.info("Generated flight data: topic={}, partition={}, offset={}, flight={}",
                                metadata.topic(), metadata.partition(), metadata.offset(), flight.getFlightNumber());
                    }
                });

                if (intervalMs > 0) {
                    TimeUnit.MILLISECONDS.sleep(intervalMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Data generation interrupted", e);
        } finally {
            producer.close();
        }
    }

    public static void main(String[] args) {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String schemaRegistryUrl = System.getenv().getOrDefault("SCHEMA_REGISTRY_URL", "http://localhost:8081");
        int messageCount = Integer.parseInt(System.getenv().getOrDefault("MESSAGE_COUNT", "100"));
        long intervalMs = Long.parseLong(System.getenv().getOrDefault("INTERVAL_MS", "1000"));
        String topic = System.getenv().getOrDefault("TOPIC", DEFAULT_TOPIC);

        AvroFlightDataGenerator generator = new AvroFlightDataGenerator(bootstrapServers, schemaRegistryUrl, topic);
        generator.generateData(messageCount, intervalMs);
    }
}
