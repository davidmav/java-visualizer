package org.javalens.visualizer.agent.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.util.IOUtils;
import org.javalens.dummyapp.consumer.DataConsumer;
import org.javalens.dummyapp.producer.RandomDataProducer;
import org.javalens.visualizer.analyzer.Analyzer;
import org.javalens.visualizer.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ITEventInstrumentationAgentTest {

    public static final String DUMMY_APP_MAIN_CLASS = "org.javalens.App";
    public static final String PRODUCE_DATA_EVEMT_NAME = "ProduceData";
    public static final String CONSUME_DATA_EVENT_NAME = "ConsumeData";

    @Test
    public void testInstrumentationOfMethods() throws IOException, InterruptedException {
        String currentDir = System.getProperty("user.dir");
        Path currentDirPath = Paths.get(currentDir);
        Path instrumentedJson = currentDirPath.resolve("instrumented_events.json");
        Path testOutput = currentDirPath.resolve("target/events");
        createConfigurationFile(instrumentedJson, testOutput);
        Process process = startDummyAppWithAgent(instrumentedJson);
        boolean exited = process.waitFor(15, TimeUnit.SECONDS);
        if (exited) {
            System.out.println("Output" + IOUtils.toString(new InputStreamReader(process.getInputStream())));
        }
        assertFalse(exited);
        process.destroy();

        List<EventBoundary> events = EventsSerializer.readEventsFromDisk(testOutput);
        List<EventBoundary> consumptionStart = new ArrayList<>();
        List<EventBoundary> consumptionEnd = new ArrayList<>();
        List<EventBoundary> productionStart = new ArrayList<>();
        List<EventBoundary> productionEnd = new ArrayList<>();
        for (EventBoundary event : events) {
            assertNotNull(event.getEventName());
            assertNotNull(event.getBoundaryType());
            assertNotNull(event.getBoundaryEpoch());
            assertNotNull(event.getBoundaryThread());
            if (event.getEventName().equals(PRODUCE_DATA_EVEMT_NAME)) {
                if (event.getBoundaryType() == EventBoundary.BoundaryTypeEnum.START) {
                    productionStart.add(event);
                } else {
                    productionEnd.add(event);
                }
            } else if (event.getEventName().equals(CONSUME_DATA_EVENT_NAME)) {
                if (event.getBoundaryType() == EventBoundary.BoundaryTypeEnum.START) {
                    consumptionStart.add(event);
                } else {
                    consumptionEnd.add(event);
                }
            } else {
                throw new IllegalArgumentException("Invalid EventName " + event.getEventName());
            }
        }
        Analyzer.main(new String[]{testOutput.toString()});
        assertTrue(consumptionStart.size() > 0);
        assertTrue(consumptionEnd.size() > 0);
        assertTrue(productionStart.size() > 0);
        assertTrue(productionEnd.size() > 0);
    }

    private static Process startDummyAppWithAgent(Path instrumentedJson) throws IOException {
        Path javaHome = Path.of(System.getProperty("java.home"));
        Path javaBin = javaHome.resolve("bin").resolve("java");
        Path outputPath = Path.of(System.getProperty("project.build.directory"));
        String artifactId = System.getProperty("project.artifactId");
        String version = System.getProperty("project.version");
        Path agentPath = outputPath.resolve(artifactId + "-" + version + "-jar-with-dependencies.jar");
        Path dummyAppPath = outputPath.resolve("dummy-test-app").resolve("dummy-test-app.jar");
        String[] CMD_ARRAY =
                {javaBin.toString(), "-javaagent:" + agentPath, "-cp", dummyAppPath.toString(),
                        "-Djavalens.configurationFile=" + instrumentedJson.toString(), DUMMY_APP_MAIN_CLASS};
        System.out.println("Running " + Arrays.toString(CMD_ARRAY));
        return new ProcessBuilder(CMD_ARRAY).start();
    }

    private static void createConfigurationFile(Path instrumentedJson, Path testOutput) throws IOException {
        InstrumentedEvents instrumentedEvents = new InstrumentedEvents();
        instrumentedEvents.addEventsItem(
                new EventFlowDefinition()
                        .eventName(CONSUME_DATA_EVENT_NAME)
                        .eventStart(
                                new MethodCriteria()
                                        .eventType(MethodCriteria.EventTypeEnum.START)
                                        .methodName("consumeData")
                                        .methodClass(DataConsumer.class.getName())
                                        .traceArguments(List.of(
                                                new MethodArgument()
                                                        .argumentPath("[0].requestId")
                                                        .argumentType(MethodArgument.ArgumentTypeEnum.METHOD)))
                        ).eventEnd(
                                new MethodCriteria()
                                        .eventType(MethodCriteria.EventTypeEnum.END)
                                        .methodName("consumeData")
                                        .methodClass(DataConsumer.class.getName())
                                        .traceArguments(List.of(
                                                new MethodArgument()
                                                        .argumentPath("[0].requestId")
                                                        .argumentType(MethodArgument.ArgumentTypeEnum.METHOD)))
                        )
        );
        instrumentedEvents.addEventsItem(
                new EventFlowDefinition()
                        .eventName(PRODUCE_DATA_EVEMT_NAME)
                        .eventStart(
                                new MethodCriteria()
                                        .eventType(MethodCriteria.EventTypeEnum.START)
                                        .methodName("produceData")
                                        .methodClass(RandomDataProducer.class.getName())
                                        .traceArguments(List.of(
                                                new MethodArgument()
                                                        .argumentPath("[0]")
                                                        .argumentType(MethodArgument.ArgumentTypeEnum.METHOD)))
                        ).eventEnd(
                                new MethodCriteria()
                                        .eventType(MethodCriteria.EventTypeEnum.END)
                                        .methodName("produceData")
                                        .methodClass(RandomDataProducer.class.getName())
                                        .traceArguments(List.of(
                                                new MethodArgument()
                                                        .argumentPath("[0]")
                                                        .argumentType(MethodArgument.ArgumentTypeEnum.METHOD)))
                        )
        );
        instrumentedEvents.setSink(new FileSystemSinkConfig()
                .chunkSize(1000)
                .path(testOutput.toString()));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(instrumentedJson.toFile(), instrumentedEvents);
    }
}
