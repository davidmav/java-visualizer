package org.javalens.visualizer.agent;

import org.javalens.visualizer.agent.events.EventSink;
import org.javalens.visualizer.agent.events.FileSystemSink;
import org.javalens.visualizer.model.*;

import java.util.List;
import java.util.Map;

public class EventRecorder {

    public static final String EVENT_IDENTIFIER_DELIMITER = "__";
    private static boolean INITIALIZED = false;

    private static EventSink SINK;

    private static EventConfigurationRepository REPOSITORY;

    public static void recordEvent(String eventName,
                                   MethodCriteriaRecord.Type eventType,
                                   String thread,
                                   String threadMatchPattern,
                                   Object targetObject,
                                   Object[] methodArguments,
                                   long epochMillis) {
        if (!INITIALIZED) {
            throw new IllegalStateException("EventRecorder hasn't been initialized yet");
        }

        if (threadMatchPattern != null && !thread.matches(threadMatchPattern)) {
            return;
        }

        Map<MethodCriteriaRecord.Type, MethodCriteriaRecord> eventConfiguration = REPOSITORY.getEventConfiguration(eventName);
        MethodCriteriaRecord methodCriteriaRecord = eventConfiguration.get(eventType);
        MethodCriteria methodCriteria = methodCriteriaRecord.methodCriteria();
        List<MethodArgument> eventArguments = methodCriteria.getEventArguments();
        List<MethodArgument> traceArguments = methodCriteria.getTraceArguments();

        String eventId = computeIdentifier(targetObject, methodArguments, methodCriteria, eventArguments);
        String traceId = computeIdentifier(targetObject, methodArguments, methodCriteria, traceArguments);

        SINK.recordEventBoundary(new EventBoundary()
                .boundaryEpoch(epochMillis)
                .boundaryThread(thread)
                .eventName(eventName)
                .boundaryType(EventBoundary.BoundaryTypeEnum.fromValue(eventType.name()))
                .eventId(eventId)
                .traceId(traceId));
    }

    private static String computeIdentifier(Object targetObject, Object[] methodArguments, MethodCriteria methodCriteria, List<MethodArgument> traceArguments) {
        String eventIdentifier = null;
        if (traceArguments != null && !traceArguments.isEmpty()) {
            try {
                eventIdentifier = constructEventIdentifier(traceArguments, targetObject, methodArguments);
            } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return eventIdentifier;
    }

    private static String constructEventIdentifier(List<MethodArgument> traceArguments,
                                                   Object targetObject,
                                                   Object[] methodArguments) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < traceArguments.size(); i++) {
            MethodArgument methodArgument = traceArguments.get(i);
            stringBuilder.append(ReflectionUtils.methodArgumentToString(methodArgument, targetObject, methodArguments));
            if (i != traceArguments.size() - 1) {
                stringBuilder.append(EVENT_IDENTIFIER_DELIMITER);
            }
        }

        return stringBuilder.toString();
    }

    static void initialize(EventConfigurationRepository repository) {
        SinkConfiguration sink = repository.getSinkConfiguration();
        if (sink instanceof FileSystemSinkConfig) {
            SINK = new FileSystemSink((FileSystemSinkConfig) sink);
        } else {
            throw new IllegalArgumentException("Only FileSystemSink is currently supported");
        }
        REPOSITORY = repository;
        INITIALIZED = true;
    }
}
