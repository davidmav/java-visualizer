package org.javalens.dummyapp.producer;

import org.javalens.dummyapp.Data;

public class RandomDataProducer {

    public Data produceData(String requestId) throws InterruptedException {
        byte[] buffer = new byte[256];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) Math.floor(256 * Math.random());
        }
        Thread.sleep((long) (Math.random() * 10));
        return new Data(requestId, buffer);
    }
}
