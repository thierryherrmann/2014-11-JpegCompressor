package com.thn.jpegcompressor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class SimpleThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger();

    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("thread-" + counter.incrementAndGet());
        return thread;
    }
}