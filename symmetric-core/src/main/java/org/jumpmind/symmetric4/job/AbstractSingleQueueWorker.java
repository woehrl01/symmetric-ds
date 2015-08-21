package org.jumpmind.symmetric4.job;

abstract public class AbstractSingleQueueWorker<T> extends AbstractQueueWorkers<T> {

    @Override
    protected int getMaxNumberOfThreads() {
        return 1;
    }
}
