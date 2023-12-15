package org.javalens.visualizer.agent.events;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.javalens.visualizer.model.EventBoundary;
import org.javalens.visualizer.model.FileSystemSinkConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.javalens.visualizer.agent.events.FileSystemSink.SERIALIZER;
import static org.junit.jupiter.api.Assertions.*;

public class UTFileSystemSinkTest {

    private FileSystemSink fileSystemSink;

    @BeforeEach
    public void setUp() {

        // Create a FileSystemSinkConfig for testing
        FileSystemSinkConfig config = new FileSystemSinkConfig();
        config.setChunkSize(2); // Set a small chunk size for testing
        String currentWorkingDirectory = System.getProperty("user.dir");
        config.setPath(currentWorkingDirectory);

        // Create the FileSystemSink instance with mocks and test config
        fileSystemSink = new FileSystemSink(config);
    }

    @AfterEach
    public void tearDown() {
        File[] matchingFlushedFiles = getMatchingFlushedFiles();
        Arrays.stream(matchingFlushedFiles).forEach(File::delete);
    }

    @Test
    @DisplayName("Record event boundary and verify if it's added to events queue")
    public void testRecordEventBoundary() {
        // Create a sample EventBoundary
        EventBoundary eventBoundary = new EventBoundary().eventName("TestEvent");
        // Record the event boundary using the FileSystemSink
        fileSystemSink.recordEventBoundary(eventBoundary);
        assertEquals(1, fileSystemSink.events.size()); // event consumed by thread reader
    }

    @Test
    @DisplayName("Record two events and verify they are flushed to a file")
    public void testFlushToFile() throws IOException, InterruptedException {
        // Create two sample EventBoundary objects
        EventBoundary event1 = new EventBoundary().eventName("TestEvent").boundaryType(EventBoundary.BoundaryTypeEnum.START);
        EventBoundary event2 =
                new EventBoundary().eventName("TestEvent").boundaryType(EventBoundary.BoundaryTypeEnum.END);

        // Record the events using the FileSystemSink
        fileSystemSink.recordEventBoundary(event1);
        fileSystemSink.recordEventBoundary(event2);
        Thread.sleep(100);
        verifyEventsFlushedToFile(event1, event2);
    }

    private void verifyEventsFlushedToFile(EventBoundary... expectedEvents) throws IOException {
        // Define the expected file path based on the config
        File[] matchingFiles = getMatchingFlushedFiles();

        if (matchingFiles != null && matchingFiles.length > 0) {
            File latestFile = matchingFiles[matchingFiles.length - 1];

            // Read the file content
            byte[] fileContent = Files.readAllBytes(latestFile.toPath());

            // Deserialize the content using Kryo
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(ArrayList.class);
            kryo.register(EventBoundary.BoundaryTypeEnum.class);
            kryo.register(EventBoundary.class);
            List<EventBoundary> flushedEvents;
            try (Input input = new Input(fileContent)) {
                flushedEvents = kryo.readObject(input, ArrayList.class, SERIALIZER);
            }
            // Verify that the flushed events match the expected events
            assertEquals(expectedEvents.length, flushedEvents.size());
            for (int i = 0; i < expectedEvents.length; i++) {
                assertEquals(expectedEvents[i], flushedEvents.get(i));
            }
        } else {
            fail("No matching files found.");
        }
    }

    private File[] getMatchingFlushedFiles() {
        FileSystemSinkConfig config = fileSystemSink.config;

        // Find the most recent file matching the pattern
        File[] matchingFiles = new File(config.getPath()).listFiles(
                (dir, name) -> name.startsWith(FileSystemSink.DEFAULT_PREFIX + "_")
        );
        return matchingFiles;
    }

    @Test
    @DisplayName("Test closing FileSystemSink")
    public void testClose() throws Exception {
        // Call the close method on FileSystemSink
        fileSystemSink.close();
        assertTrue(fileSystemSink.executorService.isShutdown());
    }
}