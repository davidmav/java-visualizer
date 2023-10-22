package org.javalens.visualizer.agent.events;

import org.javalens.visualizer.model.EventBoundary;

public interface EventSink {
    void recordEventBoundary(EventBoundary eventBoundary);
}
