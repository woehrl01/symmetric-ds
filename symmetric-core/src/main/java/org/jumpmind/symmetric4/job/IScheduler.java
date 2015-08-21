package org.jumpmind.symmetric4.job;

public interface IScheduler {

    public void schedule(String name, String cron, Runnable run);
    
    public void schedule(String name, long periodInMs, Runnable run);
    
    public void scheduleImmediate(String name, Runnable run);
    
}
