package org.javalens.dummyapp.consumer;

import org.javalens.dummyapp.Data;

public class DataConsumer {

    public void consumeData(Data data) throws InterruptedException {
        Thread.sleep((long) (10 * Math.random()));
    }
}
