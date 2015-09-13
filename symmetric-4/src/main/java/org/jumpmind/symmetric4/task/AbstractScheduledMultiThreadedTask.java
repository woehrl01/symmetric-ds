package org.jumpmind.symmetric4.task;

import java.util.List;

import org.jumpmind.symmetric4.IApplicationContext;

abstract public class AbstractScheduledMultiThreadedTask<T> extends AbstractScheduledTask {

    AbstractMultiThreadedTask<T> workers;
    
    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        super.init(serviceContext, scheduler);
        workers = new AbstractMultiThreadedTask<T>() {
            @Override
            public String getName() {
                return AbstractScheduledMultiThreadedTask.this.getName();
            }
            
            @Override
            protected void doWork(T work) {
                AbstractScheduledMultiThreadedTask.this.doWork(work);
            }
        };
        workers.init(serviceContext, scheduler); 
    }
    
    @Override
    protected void run() {
        List<T> work = getWorkForThreads();
        for (T t : work) {
            workers.queueWork(t);
        }
    }
    
    abstract protected List<T> getWorkForThreads();
    
    abstract protected void doWork(T work);
}
