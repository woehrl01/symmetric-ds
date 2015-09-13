package org.jumpmind.symmetric4.task;

abstract public class AbstractSingleThreadedTask<T> extends AbstractMultiThreadedTask<T> {

    @Override
    protected int getMaxNumberOfThreads() {
        return 1;
    }
}
