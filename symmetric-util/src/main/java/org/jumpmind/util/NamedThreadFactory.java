package org.jumpmind.util;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    
    String poolName;
    
    final AtomicInteger threadNumber = new AtomicInteger(1);

    public NamedThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName(poolName + threadNumber.getAndIncrement());
        t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
