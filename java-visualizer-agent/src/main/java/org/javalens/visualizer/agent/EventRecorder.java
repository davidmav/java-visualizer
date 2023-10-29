package org.javalens.visualizer.agent;

import org.javalens.visualizer.agent.events.EventSink;
import org.javalens.visualizer.model.EventBoundary;

public class EventRecorder {

    private static boolean INITIALIZED = false;

    private static EventSink SINK;

    public enum EventType {
        START, END
    }

    public static void recordEvent(String eventName, EventType eventType, String thread, long epochMillis) {
        if (!INITIALIZED) {
            throw new IllegalStateException("EventRecorder hasn't been initialized yet");
        }

        SINK.recordEventBoundary(new EventBoundary()
                .boundaryEpoch(epochMillis)
                .boundaryThread(thread)
                .eventName(eventName)
                .boundaryType(EventBoundary.BoundaryTypeEnum.fromValue(eventType.name())));
    }

    public static void recordEvent() {
        if (!INITIALIZED) {
            throw new IllegalStateException("EventRecorder hasn't been initialized yet");
        }
    }

    static void initialize(EventSink sink) {
        SINK = sink;
        INITIALIZED = true;
    }
}
