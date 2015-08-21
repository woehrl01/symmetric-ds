package org.jumpmind.symmetric4.job;

import java.util.List;

import org.jumpmind.symmetric4.IApplicationContext;

abstract public class AbstractScheduledWorkers<T> extends AbstractScheduledWorker {

    AbstractQueueWorkers<T> workers;
    
    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        super.init(serviceContext, scheduler);
        workers = new AbstractQueueWorkers<T>() {
            @Override
            public String getJobName() {
                return AbstractScheduledWorkers.this.getJobName();
            }
            
            @Override
            protected void work(T work) {
                AbstractScheduledWorkers.this.work(work);
            }
        };
        workers.init(serviceContext, scheduler); 
    }
    
    @Override
    protected void run() {
        List<T> work = getWork();
        for (T t : work) {
            workers.queue(t);
        }
    }
    
    abstract protected List<T> getWork();
    
    abstract protected void work(T work);
}
