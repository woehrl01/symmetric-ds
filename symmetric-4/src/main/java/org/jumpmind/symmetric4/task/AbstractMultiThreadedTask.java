package org.jumpmind.symmetric4.task;

abstract public class AbstractMultiThreadedTask<T> extends AbstractTask {

    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
    }
    
    public void queueWork(T work) {
    }
    
    abstract protected void doWork(T work);    
    
}
