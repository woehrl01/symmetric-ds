package org.jumpmind.symmetric4.job.outgoing;

import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric4.job.AbstractSingleQueueWorker;

public class CreateBatchesWorker extends AbstractSingleQueueWorker<Data> {
    
    // It is ok to keep state because this is a "single" queue worker
    
    @Override
    public String getJobName() {
        return "Create Batches Job";
    }
    
    @Override
    protected void work(Data work) {
        // do the work that the router service does today to create batches
    }
}
