package org.jumpmind.symmetric4.job;

import java.io.Serializable;

abstract public class AbstractReservedQueueWorkers<T extends Serializable, W> extends AbstractWorker {

    public boolean reserve(T reservationToken) {
        return true;
    }

    public void free(T reservationToken) {

    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
    }

    public void queue(T reservationToken, W work) {
        // throw error if a bad reservation token is passed in
    }

    abstract protected void work(T reservationToken, W work);
}
