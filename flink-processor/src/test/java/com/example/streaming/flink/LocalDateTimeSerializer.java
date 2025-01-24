package com.example.streaming.flink;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LocalDateTimeSerializer extends Serializer<LocalDateTime> implements Serializable {
    private static final long serialVersionUID = 1L;

    public LocalDateTimeSerializer() {
        super(false, true); // immutable, accepts null
    }

    @Override
    public void write(Kryo kryo, Output output, LocalDateTime dateTime) {
        if (dateTime == null) {
            output.writeBoolean(false);
            return;
        }
        output.writeBoolean(true);
        output.writeLong(dateTime.toEpochSecond(ZoneOffset.UTC));
        output.writeInt(dateTime.getNano());
    }

    @Override
    public LocalDateTime read(Kryo kryo, Input input, Class<LocalDateTime> type) {
        if (!input.readBoolean()) {
            return null;
        }
        long epochSecond = input.readLong();
        int nano = input.readInt();
        return LocalDateTime.ofEpochSecond(epochSecond, nano, ZoneOffset.UTC);
    }
}
