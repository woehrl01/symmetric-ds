package org.jumpmind.symmetric4.job;

abstract public class AbstractQueueWorkers<T> extends AbstractWorker {

    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
    }
    
    public void queue(T work) {
    }
    
    abstract protected void work(T work);
    
    
}
