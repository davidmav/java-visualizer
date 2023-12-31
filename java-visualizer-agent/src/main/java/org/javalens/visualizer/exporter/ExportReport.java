package org.javalens.visualizer.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.javalens.visualizer.agent.events.EventsSerializer;
import org.javalens.visualizer.model.ChromeTraceCompleteEvent;
import org.javalens.visualizer.model.ChromeTraceEventArgs;
import org.javalens.visualizer.model.Event;
import org.javalens.visualizer.model.EventBoundary;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportReport {

    public static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.HTML;

    enum OutputFormat {
        JSON,
        HTML
    }

    public static final String HELP_OPTION = "help";
    public static final String SOURCE_OPTION = "source";
    public static final String DESTINATION_OPTION = "destination";
    public static final String MIN_EVENT_COUNT_OPTION = "min-event-count";
    public static final String OUTPUT_FORMAT_OPTION = "output-format";
    public static final String PERCENTILE_OPTION = "percentile";
    public static final String LIMIT_OPTION = "limit";
    public static final int DEFAULT_LIMIT = 1000;
    public static final String REPORT_NAME_DATETIME_FORMAT = "yyyyMMddHHmmss";
    public static final String HTML_REPORT_NAME_SUFFIX = "_report.html";
    public static final String JSON_REPORT_NAME_SUFFIX = "_report.json";

    public static void main(String[] args) throws IOException {

        // create the parser
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null;
        Options argOptions = createArgOptions();
        try {
            // parse the command line arguments
            line = parser.parse(argOptions, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Wrong options provided.  Reason: " + exp.getMessage());
            printHelp(argOptions);
            System.exit(1);
        }

        if (line.hasOption(HELP_OPTION)) {
            printHelp(argOptions);
        }

        String rawSourcePath = line.getOptionValue(SOURCE_OPTION);
        URI sourceURI = URI.create(rawSourcePath);

        if (sourceURI.getScheme() != null && sourceURI.getScheme().equals("file")) {
            System.err.println("Only local file system source is supported right now.");
            System.exit(1);
        }

        String rawDestinationPath = line.getOptionValue(DESTINATION_OPTION);
        Path destinationPath = Path.of(rawDestinationPath);
        if (!Files.exists(destinationPath)) {
            System.err.println("Destination path " + rawDestinationPath + " does not exist or invalid");
            System.exit(1);
        }

        String sourcePathString = sourceURI.getPath();
        Path sourcePath = Paths.get(sourcePathString);
        if (!Files.exists(sourcePath)) {
            System.err.println("Source path " + rawDestinationPath + " does not exist or invalid");
            System.exit(1);
        }

        System.out.println("Reading events from " + sourcePathString);
        List<EventBoundary> eventBoundaries = EventsSerializer.readEventsFromDisk(sourcePath);
        if (eventBoundaries.isEmpty()) {
            System.err.println("Source path " + rawDestinationPath + " does not contain any valid events");
            System.exit(1);
        }

        int minEvents = -1;
        String minEventsOption = line.getOptionValue(MIN_EVENT_COUNT_OPTION);
        if (minEventsOption != null) {
            try {
                minEvents = Integer.parseInt(minEventsOption);
            } catch (NumberFormatException e) {
                System.err.println("Invalid value specified for " + MIN_EVENT_COUNT_OPTION + " , the value must be a " +
                        "a positive number");
                System.exit(1);
            }
        }

        double percentile = -1;
        String percentileOption = line.getOptionValue(PERCENTILE_OPTION);
        if (percentileOption != null) {
            try {
                percentile = Double.parseDouble(percentileOption);
                if (percentile < 0 || percentile > 100) {
                    throw new NumberFormatException("Invalid range");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid value specified for percentile, the value must be a number between 0 and " +
                        "100");
                System.exit(1);
            }
        }

        String limitOption = line.getOptionValue(LIMIT_OPTION);
        int limit = DEFAULT_LIMIT;
        if (limitOption != null) {
            try {
                limit = Integer.parseInt(limitOption);
            } catch (NumberFormatException e) {
                System.err.println("Invalid value specified for limit, the value must be an integer");
                System.exit(1);
            }
        }

        String outputType = line.getOptionValue(OUTPUT_FORMAT_OPTION);
        OutputFormat outputFormat = DEFAULT_OUTPUT_FORMAT;
        if (outputType != null) {
            outputFormat = OutputFormat.valueOf(outputType);
        }
        produceReport(outputFormat, eventBoundaries, percentile, limit, destinationPath, minEvents);
    }

    private static void produceReport(OutputFormat outputFormat, List<EventBoundary> eventBoundaries, Double percentile, int limit,
                                      Path destination, int minEvents) throws IOException {
        DateFormat df = new SimpleDateFormat(REPORT_NAME_DATETIME_FORMAT);
        Path reportPath = destination.resolve(df.format(new Date()) + (outputFormat.equals(OutputFormat.HTML) ?
                HTML_REPORT_NAME_SUFFIX : JSON_REPORT_NAME_SUFFIX));
        try (InputStream resourceAsStream = ExportReport.class.getResourceAsStream("/visualizer-embedded.html");
             OutputStream outputStream = new FileOutputStream(reportPath.toFile())) {
            if (resourceAsStream == null) {
                throw new RuntimeException("Could not find report template");
            }
            String reportTemplate = new String(resourceAsStream.readAllBytes());

            List<Event> events = parseEvents(eventBoundaries);
            List<List<Event>> groupedByTraceIdEvents = getGroupedByTraceIdEvents(events);
            if (minEvents > 0) {
                groupedByTraceIdEvents =
                        groupedByTraceIdEvents.stream().filter(item -> item.size() >= minEvents).collect(Collectors.toList());
            }

            events = getLimitedEventsByPercentile(percentile, limit, groupedByTraceIdEvents);
            ObjectMapper objectMapper = new ObjectMapper();
            List<?> finalEvents;
            if (outputFormat.equals(OutputFormat.JSON)) {
                finalEvents = events.stream().map(item -> new ChromeTraceCompleteEvent()
                        .name(item.getEventName())
                        .cat(item.getEventName())
                        .pid(item.getProcessId())
                        .ph("X")
                        .dur((item.getEventEndEpoch() - item.getEventStartEpoch()) * 1000)
                        .ts(item.getEventStartEpoch())
                        .tid(item.getThreadName() + " (" + item.getThreadId() + ")")
                        .args(new ChromeTraceEventArgs()
                                .eventId(item.getEventId())
                                .traceId(item.getTraceId())
                                .threadName(item.getThreadName()))).collect(Collectors.toList());
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, finalEvents);
            } else {
                finalEvents = events;
                String eventsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalEvents);
                String report = reportTemplate.replace("let jsonData = []", "let jsonData = " + eventsJson);
                outputStream.write(report.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static List<Event> getLimitedEventsByPercentile(Double percentile, int limit, List<List<Event>> groupedByTraceIdEvents) {
        List<Event> events;
        Stream<List<Event>> eventsStream;
        if (percentile >= 0) {
            List<Long> sortedLatencies = groupedByTraceIdEvents.stream()
                    .map(ExportReport::getTraceLatency)
                    .sorted().collect(Collectors.toList());

            List<Long> percentiles = new ArrayList<>(1000);

            for (int i = 1; i <= 1000; i++) {
                percentiles.add(calculatePercentile(sortedLatencies, (double) i / 10));
            }
            Long percentileThreshold = percentiles.get((int) Math.floor(percentile * 10));
            eventsStream =
                    groupedByTraceIdEvents.stream().filter(item -> getTraceLatency(item) >= percentileThreshold).limit(limit);
        } else {
            eventsStream =
                    groupedByTraceIdEvents.stream().limit(limit);
        }

        events = eventsStream
                .sorted(Comparator.comparingLong(k -> k.get(0).getEventStartEpoch()))
                .flatMap(List::stream).collect(Collectors.toList());
        return events;
    }

    private static long getTraceLatency(List<Event> item) {
        return item.get(item.size() - 1).getEventEndEpoch() - item.get(0).getEventStartEpoch();
    }

    public static long calculatePercentile(List<Long> latencies, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size()) - 1;
        return latencies.get(index);
    }

    private static List<List<Event>> getGroupedByTraceIdEvents(List<Event> events) {
        List<List<Event>> groupedByTraceId = new ArrayList<>();
        Map<String, List<Event>> groupedByTraceIdMap = new ConcurrentHashMap<>();
        events.parallelStream().forEach(item -> {
            if (item.getTraceId() == null) {
                groupedByTraceId.add(List.of(item));
            } else {
                List<Event> currentTrace = groupedByTraceIdMap.computeIfAbsent(item.getTraceId(), k -> new ArrayList<>());
                currentTrace.add(item);
            }
        });
        groupedByTraceId.addAll(groupedByTraceIdMap.values());
        return groupedByTraceId;
    }

    private static void printHelp(Options argOptions) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp("export-report", argOptions);
    }

    private static List<Event> parseEvents(List<EventBoundary> eventBoundaries) {
        Map<String, Event> events = new ConcurrentHashMap<>();
        eventBoundaries.parallelStream().forEach(item -> processEventBoundaries(events, item));
        return events.values().stream()
                .filter(item -> item.getEventStartEpoch() != null && item.getEventEndEpoch() != null)
                .collect(Collectors.toList());
    }

    private static void processEventBoundaries(Map<String, Event> events, EventBoundary item) {
        String id = item.getEventName() + "_" + item.getEventId();
        Event event = events.computeIfAbsent(id, k -> new Event());
        event.setEventName(item.getEventName());
        if (item.getBoundaryType() == EventBoundary.BoundaryTypeEnum.START) {
            event.setEventStartEpoch(item.getBoundaryEpoch());
        } else {
            event.setEventEndEpoch(item.getBoundaryEpoch());
        }
        event.setThreadName(item.getBoundaryThread());
        event.setEventId(item.getEventId());
        event.setTraceId(item.getTraceId());
        event.setProcessId(item.getProcessId());
        event.setThreadId(item.getBoundaryThreadId());
    }

    private static Options createArgOptions() {
        Options options = new Options();
        options.addRequiredOption("s", SOURCE_OPTION, true, "The path of the events payload, file:// or s3://");
        options.addOption("l", "limit", true, "Limit the number of exported traces (1000 by default)");
        options.addOption("p", "percentile", true, "Filter events by overall latency percentile");
        options.addRequiredOption("d", DESTINATION_OPTION, true, "The path where to store the output report, local file " +
                "system only");
        options.addOption("m", MIN_EVENT_COUNT_OPTION, true, "The minimum number of events per trace");
        options.addOption("o", OUTPUT_FORMAT_OPTION, true, "Output format, Chrome Trace Json (json) or HTML (html)");
        options.addOption("h", HELP_OPTION, false, "Displays this help message");
        return options;
    }
}
