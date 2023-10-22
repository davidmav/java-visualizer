package org.javalens.visualizer.agent.events;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.javalens.visualizer.model.EventBoundary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UTEventsSerializerTest {

    private Kryo kryo;
    private EventsSerializer serializer;

    @BeforeEach
    public void setUp() {
        kryo = new Kryo();
        serializer = new EventsSerializer();
    }

    @Test
    public void testSerializationAndDeserialization() {
        // Create a list of EventBoundary objects
        List<EventBoundary> eventBoundaries = new ArrayList<>();
        eventBoundaries.add(new EventBoundary()
                .boundaryEpoch(123456L)
                .eventName("Event1")
                .boundaryThread("Thread1")
                .boundaryType(EventBoundary.BoundaryTypeEnum.START));
        eventBoundaries.add(new EventBoundary()
                .boundaryEpoch(789012L)
                .eventName("Event2")
                .boundaryThread("Thread2")
                .boundaryType(EventBoundary.BoundaryTypeEnum.END));

        // Serialize the list
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        serializer.write(kryo, output, eventBoundaries);
        output.close();
        byte[] serializedData = outputStream.toByteArray();

        // Deserialize the data back into a list
        ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
        Input input = new Input(inputStream);
        List<EventBoundary> deserializedEventBoundaries = serializer.read(kryo, input, null);

        // Verify that the deserialized list matches the original list
        assertEquals(eventBoundaries.size(), deserializedEventBoundaries.size());
        for (int i = 0; i < eventBoundaries.size(); i++) {
            EventBoundary originalEvent = eventBoundaries.get(i);
            EventBoundary deserializedEvent = deserializedEventBoundaries.get(i);
            assertEquals(originalEvent, deserializedEvent);
        }
    }

    @Test
    public void testSerializationWithNullValues() {
        // Create a list with null values
        List<EventBoundary> eventBoundaries = new ArrayList<>();
        eventBoundaries.add(null);

        // Serialize the list
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        serializer.write(kryo, output, eventBoundaries);
        output.close();
        byte[] serializedData = outputStream.toByteArray();

        // Deserialize the data back into a list
        ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
        Input input = new Input(inputStream);
        List<EventBoundary> deserializedEventBoundaries = serializer.read(kryo, input, null);

        // Verify that the deserialized list contains null values
        assertEquals(eventBoundaries.size(), deserializedEventBoundaries.size());
        assertNull(deserializedEventBoundaries.get(0));
    }
}