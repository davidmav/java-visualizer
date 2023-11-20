package org.javalens.visualizer.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javalens.visualizer.agent.events.EventsSerializer;
import org.javalens.visualizer.model.Event;
import org.javalens.visualizer.model.EventBoundary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Analyzer {

    public static void main(String[] args) throws IOException {
        String pathStr = args[0];
        Path path = Paths.get(pathStr);
        System.out.println("Reading events from " + pathStr);
        List<EventBoundary> eventBoundaries = EventsSerializer.readEventsFromDisk(path);
        Map<String, Event> events = new ConcurrentHashMap<>();
        eventBoundaries.parallelStream().forEach(item -> {
            String id = item.getEventName() + "_" + item.getEventIdentifier();
            Event event = events.computeIfAbsent(id, k -> new Event());
            event.setEventName(item.getEventName());
            if (item.getBoundaryType() == EventBoundary.BoundaryTypeEnum.START) {
                event.setEventStartEpoch(item.getBoundaryEpoch());
            } else {
                event.setEventEndEpoch(item.getBoundaryEpoch());
            }
            event.setThread(item.getBoundaryThread());
            event.setEventId(item.getEventIdentifier());

        });
        List<Event> values = new ArrayList<>(events.values());
        values.stream().sorted(Comparator.comparing(Event::getEventEndEpoch));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("./export.json"), values);
    }
}
