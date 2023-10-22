package org.javalens.visualizer.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javalens.visualizer.agent.events.EventSink;
import org.javalens.visualizer.agent.events.FileSystemSink;
import org.javalens.visualizer.model.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventInstrumentationAgent {

    public static final String JAVALENS_CONFIGURATION_FILE = "javalens.configurationFile";

    private static InstrumentedEvents INSTRUMENTED_EVENTS;

    static EventSink EVENTS_SINK;

    public static void premain(String agentArgs, Instrumentation instrumentation) throws IOException {
        INSTRUMENTED_EVENTS = loadInstrumentedEvents();
        SinkConfiguration sink = INSTRUMENTED_EVENTS.getSink();
        if (sink instanceof FileSystemSinkConfig) {
            EVENTS_SINK = new FileSystemSink((FileSystemSinkConfig) sink);
        } else {
            throw new IllegalArgumentException("Only FileSystemSink is currently supported");
        }

        EventRecorder.initialize(EVENTS_SINK);

        List<MethodCriteriaRecord> methodCriteriaRecords = validateEvents();

        // Add your event instrumentation logic here
        // You can use instrumentation.addTransformer() to instrument classes
        // and attach event listeners or hooks
        instrumentation.addTransformer(new JavaLensClassTransformer(methodCriteriaRecords));
    }

    private static List<MethodCriteriaRecord> validateEvents() {
        List<MethodCriteriaRecord> methodCriteriaRecords = new ArrayList<>();
        Set<String> processedEvents = new HashSet<>();
        for (EventFlowDefinition eventFlowDefinition : INSTRUMENTED_EVENTS.getEvents()) {
            EventCriteria eventStart = eventFlowDefinition.getEventStart();
            EventCriteria eventEnd = eventFlowDefinition.getEventEnd();
            String eventName = eventFlowDefinition.getEventName();
            if (processedEvents.contains(eventName)) {
                throw new IllegalArgumentException("Two events are defined with the same name: " + eventName + ", " +
                        "event names must be unique");
            }
            processedEvents.add(eventName);

            if (!eventStart.getClass().equals(eventEnd.getClass())) {
                throw new IllegalArgumentException("Invalid criteria for event" + eventName + ", Event Start and End " +
                        "criteria must be of the same type");
            }

            if (eventStart instanceof MethodCriteria) {
                methodCriteriaRecords.add(new MethodCriteriaRecord(eventName, MethodCriteriaRecord.Type.START,
                        (MethodCriteria) eventStart));
                methodCriteriaRecords.add(new MethodCriteriaRecord(eventName, MethodCriteriaRecord.Type.END,
                        (MethodCriteria) eventEnd));
            }
        }
        return methodCriteriaRecords;
    }

    private static InstrumentedEvents loadInstrumentedEvents() throws IOException {
        String configurationFilePath = System.getProperty(JAVALENS_CONFIGURATION_FILE);
        if (configurationFilePath == null) {
            throw new IllegalArgumentException("Please javalens.configurationFile Java System Property with a " +
                    "configuration file to use the JavaLens Agent");
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileInputStream fileInputStream = new FileInputStream(configurationFilePath)) {
            return objectMapper.readValue(fileInputStream,
                    InstrumentedEvents.class);
        }
    }

}
