package com.example.streaming.flink.generator;

import com.example.streaming.model.Flight;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom Flink source that generates random flight data.
 * This source implements CheckpointedFunction to support fault tolerance.
 */
@Slf4j
public class DataGeneratorSource extends RichSourceFunction<Flight> implements CheckpointedFunction {

    private static final long serialVersionUID = 1L;

    // Constants for data generation
    private static final String[] AIRPORTS = {"JFK", "LAX", "ORD", "DFW", "DEN", "SFO", "SEA", "LAS", "MCO", "EWR"};
    private static final String[] AIRLINES = {"AA", "UA", "DL", "WN", "AS", "B6", "NK", "F9"};
    private static final String[] STATUS = {"SCHEDULED", "DELAYED", "DEPARTED", "ARRIVED", "CANCELLED"};

    // Configuration properties
    private final int maxCount;
    private final long delayBetweenRecords;
    private final boolean emitWatermarks;

    // State for checkpointing
    private transient ListState<Integer> countState;
    private final AtomicInteger count;

    // Utilities for data generation
    private transient volatile boolean isRunning;
    private transient Faker faker;
    private transient Random random;

    /**
     * Creates a new DataGeneratorSource with the specified configuration.
     *
     * @param properties Configuration properties
     */
    public DataGeneratorSource(Properties properties) {
        this.maxCount = Integer.parseInt(properties.getProperty("generator.count", "1000"));
        this.delayBetweenRecords = 1000 / Integer.parseInt(properties.getProperty("generator.rate", "10"));
        this.emitWatermarks = Boolean.parseBoolean(properties.getProperty("generator.emit.watermarks", "true"));
        this.count = new AtomicInteger(0);
        
        // Initialize faker and random here to avoid NullPointerException when calling generateFlight directly
        this.faker = new Faker();
        this.random = new Random();
    }

    @Override
    public void run(SourceContext<Flight> ctx) throws Exception {
        this.isRunning = true;
        
        // These are already initialized in the constructor, but we'll reinitialize them here
        // in case they were lost during serialization/deserialization
        if (this.faker == null) {
            this.faker = new Faker();
        }
        if (this.random == null) {
            this.random = new Random();
        }

        while (isRunning && (maxCount <= 0 || count.get() < maxCount)) {
            // Generate a flight record
            Flight flight = generateFlight();

            // Use synchronized block to ensure consistency with watermarks
            synchronized (ctx.getCheckpointLock()) {
                ctx.collectWithTimestamp(flight, flight.getScheduledDeparture());
                if (emitWatermarks) {
                    ctx.emitWatermark(new Watermark(flight.getScheduledDeparture()));
                }
                count.incrementAndGet();
            }

            // Control generation rate
            if (delayBetweenRecords > 0) {
                Thread.sleep(delayBetweenRecords);
            }
        }

        log.info("Data generation completed. Generated {} records.", count.get());
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    /**
     * Generates a random flight record.
     * 
     * @return A Flight object with random data
     */
    public Flight generateFlight() {
        // Ensure faker and random are initialized
        if (faker == null) {
            faker = new Faker();
        }
        if (random == null) {
            random = new Random();
        }
        
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

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        countState.clear();
        countState.add(count.get());
        log.debug("Checkpoint created at count: {}", count.get());
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        ListStateDescriptor<Integer> descriptor = new ListStateDescriptor<>(
                "generator-state", Integer.class);
        countState = context.getOperatorStateStore().getListState(descriptor);

        // Restore state if available
        if (context.isRestored()) {
            for (Integer value : countState.get()) {
                count.set(value);
            }
            log.info("Restored state with count: {}", count.get());
        }
    }
}
