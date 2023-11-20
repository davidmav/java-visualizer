package org.javalens.visualizer.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javalens.visualizer.model.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

class EventConfigurationRepository {

    private static final String JAVALENS_CONFIGURATION_FILE = "javalens.configurationFile";

    private final Map<String, Map<String, List<MethodCriteriaRecord>>> methodCriteriaMap;
    private final Map<String, Map<MethodCriteriaRecord.Type, MethodCriteriaRecord>> eventMap;

    private final SinkConfiguration sinkConfiguration;

    EventConfigurationRepository() throws IOException {
        InstrumentedEvents instrumentedEvents = loadInstrumentedEvents();
        this.sinkConfiguration = instrumentedEvents.getSink();
        List<MethodCriteriaRecord> methodCriteriaRecords = loadAndValidate(instrumentedEvents);
        this.methodCriteriaMap = new HashMap<>();
        this.eventMap = new HashMap<>();
        methodCriteriaRecords.forEach(this::indexInstrumentedMethods);
        // Turn the lists to be immutable
        Collection<Map<String, List<MethodCriteriaRecord>>> values = methodCriteriaMap.values();
        values.forEach(map -> map.keySet().forEach(key -> map.put(key, Collections.unmodifiableList(map.get(key)))));
    }

    private void indexInstrumentedMethods(MethodCriteriaRecord methodCriteriaRecord) {
        MethodCriteria methodCriteria = methodCriteriaRecord.methodCriteria();
        Map<String, List<MethodCriteriaRecord>> currentClassMethodCriteria =
                this.methodCriteriaMap.computeIfAbsent(methodCriteria.getMethodClass(),
                        k -> new HashMap<>());
        String methodName = methodCriteria.getMethodName();
        List<MethodCriteriaRecord> methodList = currentClassMethodCriteria.computeIfAbsent(methodName, k -> new ArrayList<>());
        methodList.add(methodCriteriaRecord);

        this.eventMap.computeIfAbsent(methodCriteriaRecord.eventName(),
                        k -> new EnumMap<>(MethodCriteriaRecord.Type.class))
                .put(methodCriteriaRecord.type(), methodCriteriaRecord);
    }

    SinkConfiguration getSinkConfiguration() {
        return sinkConfiguration;
    }

    boolean containsClass(String type) {
        return methodCriteriaMap.containsKey(type);
    }

    Map<MethodCriteriaRecord.Type, MethodCriteriaRecord> getEventConfiguration(String eventName) {
        return this.eventMap.get(eventName);
    }

    Map<String, List<MethodCriteriaRecord>> getClassInstrumentedMethods(String type) {
        return Collections.unmodifiableMap(methodCriteriaMap.get(type));
    }

    private List<MethodCriteriaRecord> loadAndValidate(InstrumentedEvents instrumentedEvents) {
        List<MethodCriteriaRecord> methodCriteriaRecords = new ArrayList<>();
        Set<String> processedEvents = new HashSet<>();
        for (EventFlowDefinition eventFlowDefinition : instrumentedEvents.getEvents()) {
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
            throw new IllegalArgumentException("Please add javalens.configurationFile Java System Property with a " +
                    "configuration file to use the JavaLens Agent");
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileInputStream fileInputStream = new FileInputStream(configurationFilePath)) {
            return objectMapper.readValue(fileInputStream,
                    InstrumentedEvents.class);
        }
    }
}
