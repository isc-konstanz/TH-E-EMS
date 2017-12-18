package org.openmuc.framework.app.thebox.control;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String name;
    private final AtomicInteger counter = new AtomicInteger(0);

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = name + counter.incrementAndGet();
        return new Thread(r, threadName);
    }

}
