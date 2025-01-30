package com.example.streaming.generator;

import com.example.streaming.model.Flight;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Generates random flight data and sends it to a Kafka topic.
 */
public class AvroFlightDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AvroFlightDataGenerator.class);
    private static final String[] AIRPORTS = {"JFK", "LAX", "ORD", "DFW", "DEN", "SFO", "SEA", "LAS", "MCO", "EWR"};
    private static final String[] AIRLINES = {"AA", "UA", "DL", "WN", "AS", "B6", "NK", "F9"};
    private static final String[] STATUS = {"SCHEDULED", "DELAYED", "DEPARTED", "ARRIVED", "CANCELLED"};
    private static final String DEFAULT_TOPIC = "flight-status-avro";

    private final KafkaProducer<String, Flight> producer;
    private final String topic;
    private final Faker faker;
    private final Random random;

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
        this.topic = topic;
        this.faker = new Faker();
        this.random = new Random();
    }

    public void generateData(int count, int delayMs) {
        try {
            for (int i = 0; i < count; i++) {
                Flight flight = generateFlight();
                ProducerRecord<String, Flight> record = new ProducerRecord<>(topic, flight.getFlightNumber(), flight);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Error sending message", exception);
                    } else {
                        logger.info("Message sent successfully to topic {} partition {} offset {}",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });

                if (delayMs > 0) {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Data generation interrupted", e);
        } finally {
            producer.close();
        }
    }

    private Flight generateFlight() {
        String airline = AIRLINES[random.nextInt(AIRLINES.length)];
        String flightNumber = airline + faker.number().numberBetween(1000, 9999);
        String origin = AIRPORTS[random.nextInt(AIRPORTS.length)];
        String destination;
        do {
            destination = AIRPORTS[random.nextInt(AIRPORTS.length)];
        } while (destination.equals(origin));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDeparture = now.plusHours(random.nextInt(24));
        LocalDateTime actualDeparture = random.nextBoolean() ? scheduledDeparture.plusMinutes(random.nextInt(120)) : null;
        String status = STATUS[random.nextInt(STATUS.length)];

        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setAirline(airline);
        flight.setOrigin(origin);
        flight.setDestination(destination);
        flight.setScheduledDeparture(scheduledDeparture.toInstant(ZoneOffset.UTC).toEpochMilli());
        if (actualDeparture != null) {
            flight.setActualDeparture(actualDeparture.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        flight.setStatus(status);

        return flight;
    }

    public static void main(String[] args) {
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String schemaRegistryUrl = System.getenv().getOrDefault("SCHEMA_REGISTRY_URL", "http://localhost:8081");
        int messageCount = Integer.parseInt(System.getenv().getOrDefault("MESSAGE_COUNT", "100"));
        long intervalMs = Long.parseLong(System.getenv().getOrDefault("INTERVAL_MS", "1000"));
        String topic = System.getenv().getOrDefault("TOPIC", DEFAULT_TOPIC);

        AvroFlightDataGenerator generator = new AvroFlightDataGenerator(bootstrapServers, schemaRegistryUrl, topic);
        generator.generateData(messageCount, (int) intervalMs);
    }
}
