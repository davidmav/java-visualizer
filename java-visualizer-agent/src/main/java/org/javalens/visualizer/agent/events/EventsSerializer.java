package org.javalens.visualizer.agent.events;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.javalens.visualizer.model.EventBoundary;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EventsSerializer extends Serializer<List<EventBoundary>> {

    public static final String EMPTY_STRING = "";

    public static List<EventBoundary> readEventsFromDisk(Path path) throws IOException {
        Kryo kryo = new Kryo();
        EventsSerializer serializer = new EventsSerializer();
        List<EventBoundary> events = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(path)) {
            walk.forEach(file -> {
                if (Files.isRegularFile(file)) {
                    try (InputStream is = new FileInputStream(file.toFile())) {
                        Input input = new Input(is);
                        events.addAll(kryo.readObject(input, ArrayList.class, serializer));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return events;
    }

    @Override
    public void write(Kryo kryo, Output output, List<EventBoundary> object) {
        if (object == null) {
            output.writeInt(0);
        } else {
            output.writeInt(object.size());
            for (EventBoundary eventBoundary : object) {
                if (eventBoundary == null) {
                    output.writeBoolean(false);
                } else {
                    output.writeBoolean(true);
                    writeString(output, eventBoundary.getEventIdentifier());
                    writeLong(output, eventBoundary.getBoundaryEpoch());
                    writeString(output, eventBoundary.getEventName());
                    writeString(output, eventBoundary.getBoundaryThread());
                    writeEnum(output, eventBoundary.getBoundaryType());
                }
            }
        }
    }

    private void writeLong(Output output, Long value) {
        output.writeLong(Objects.requireNonNullElse(value, -1L), false);
    }

    private void writeEnum(Output output, EventBoundary.BoundaryTypeEnum boundaryType) {
        if (boundaryType != null) {
            output.writeInt(boundaryType.ordinal());
        } else {
            output.writeInt(-1);
        }
    }

    private void writeString(Output output, String value) {
        if (value == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(value.length());
            if (value.length() > 0) {
                output.writeBytes(value.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public List<EventBoundary> read(Kryo kryo, Input input, Class<? extends List<EventBoundary>> type) {
        int size = input.readInt();
        if (size == 0) {
            return Collections.emptyList();
        } else {
            List<EventBoundary> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                boolean nonNull = input.readBoolean();
                if (nonNull) {
                    String eventIdentifier = readString(input);
                    Long epoch = readLong(input);
                    String eventName = readString(input);
                    String boundaryThread = readString(input);
                    EventBoundary.BoundaryTypeEnum boundaryTypeEnum = readEnum(input);
                    result.add(new EventBoundary()
                            .eventIdentifier(eventIdentifier)
                            .boundaryEpoch(epoch)
                            .eventName(eventName)
                            .boundaryThread(boundaryThread)
                            .boundaryType(boundaryTypeEnum));
                } else {
                    result.add(null);
                }
            }
            return result;
        }
    }

    private Long readLong(Input input) {
        long value = input.readLong(false);
        if (value == -1) {
            return null;
        } else {
            return value;
        }
    }

    private EventBoundary.BoundaryTypeEnum readEnum(Input input) {
        int ordinal = input.readInt();
        if (ordinal < 0) {
            return null;
        } else {
            return EventBoundary.BoundaryTypeEnum.values()[ordinal];
        }
    }

    private String readString(Input input) {
        int length = input.readInt();
        if (length < 0) {
            return null;
        } else if (length == 0) {
            return EMPTY_STRING;
        } else {
            return new String(input.readBytes(length), StandardCharsets.UTF_8);
        }
    }
}
