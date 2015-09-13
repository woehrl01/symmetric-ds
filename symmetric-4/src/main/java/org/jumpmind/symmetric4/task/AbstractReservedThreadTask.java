package org.jumpmind.symmetric4.task;

import java.io.Serializable;

abstract public class AbstractReservedThreadTask<T extends Serializable, W> extends AbstractTask {

    public boolean reserveThread(T reservationToken) {
        return true;
    }

    public void freeThread(T reservationToken) {

    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
    }

    public void queueWork(T reservationToken, W work) {
        // throw error if a bad reservation token is passed in
    }

    abstract protected void doWork(T reservationToken, W work);
}
