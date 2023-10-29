package org.javalens.visualizer.agent.events;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.javalens.visualizer.model.EventBoundary;
import org.javalens.visualizer.model.FileSystemSinkConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileSystemSink implements EventSink, AutoCloseable {

    public static final String DEFAULT_PREFIX = "javalens";
    public static final EventsSerializer SERIALIZER = new EventsSerializer();
    protected BlockingQueue<EventBoundary> events;
    protected final FileSystemSinkConfig config;
    protected final ExecutorService executorService;

    protected boolean stop;

    public FileSystemSink(FileSystemSinkConfig config) {
        this.events = new LinkedBlockingQueue<>();
        this.config = config;
        this.stop = false;
        this.executorService = Executors.newFixedThreadPool(1);
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.register(EventBoundary.class);
        kryo.register(EventBoundary.BoundaryTypeEnum.class);
        kryo.register(ArrayList.class);
        executorService.submit(() -> {
            List<EventBoundary> currentChunk = new ArrayList<>(config.getChunkSize());
            while (!stop) {
                EventBoundary poll;
                try {
                    if ((poll = events.poll(100, TimeUnit.MILLISECONDS)) != null) {
                        currentChunk.add(poll);
                    }
                } catch (InterruptedException ignored) {

                }
                if (currentChunk.size() == config.getChunkSize()) {
                    try {
                        flushChunk(kryo, currentChunk);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void flushChunk(Kryo kryo, List<EventBoundary> currentChunk) throws IOException {
        Path outputPath = Paths.get(config.getPath());
        String prefix;
        if (config.getFilePrefix() != null) {
            prefix = config.getFilePrefix();
        } else {
            prefix = DEFAULT_PREFIX;
        }
        outputPath = outputPath.resolve(prefix + "_" + System.currentTimeMillis());
        Path parentFolder = outputPath.getParent();
        if (!Files.exists(parentFolder)) {
            Files.createDirectories(parentFolder);
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile());
             Output output = new Output(fileOutputStream)) {

            kryo.writeObject(output, currentChunk, SERIALIZER);
            currentChunk.clear();
        } catch (Exception e) {
            System.err.println("Error while flushing events");
            e.printStackTrace();
        }
    }

    @Override
    public void recordEventBoundary(EventBoundary eventBoundary) {
        this.events.add(eventBoundary);
    }

    @Override
    public void close() throws Exception {
        this.stop = true;
        this.executorService.shutdown();
        this.executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
}
