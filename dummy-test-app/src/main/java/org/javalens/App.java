package org.javalens;

import org.javalens.dummyapp.Data;
import org.javalens.dummyapp.consumer.DataConsumer;
import org.javalens.dummyapp.producer.RandomDataProducer;

import java.util.UUID;
import java.util.concurrent.*;

/**
 * Hello world!
 *
 */
public class App 
{
    private static DataConsumer dataConsumer;
    private static RandomDataProducer dataProducer;

    private static ExecutorService executorService;
    private static boolean shutdown = false;

    static class ShutdownHook extends Thread {

        public void run() {
            shutdown = true;
            executorService.shutdown();
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    public static void main( String[] args ) throws InterruptedException {
        initializeService();
        ShutdownHook hook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);
        synchronized (hook) {
            hook.wait();
        }
    }

    private static void initializeService() throws InterruptedException {
        // Sleep at least 5 seconds
        Thread.sleep(5000 + (long) (10000 * Math.random()));

        dataConsumer = new DataConsumer();
        dataProducer = new RandomDataProducer();

        BlockingQueue<Data> queue = new LinkedBlockingQueue<>();

        executorService = Executors.newFixedThreadPool(50);
        for (int i=0; i<25;i++) {
            executorService.submit(() -> {
                while (!shutdown) {
                    String requestId = UUID.randomUUID().toString();
                    try {
                        for (int i1 = 0; i1 < 10; i1++) {
                            Data o = dataProducer.produceData(requestId, i1);
                            queue.add(o);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        for (int i=0; i<25;i++) {
            executorService.submit(() -> {
                while (!shutdown) {
                    Data poll = null;
                    try {
                        poll = queue.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }
                    if (poll != null) {
                        try {
                            dataConsumer.consumeData(poll);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
        }

    }
}
