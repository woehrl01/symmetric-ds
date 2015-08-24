package org.jumpmind.symmetric4.job.outgoing;

import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric4.IApplicationContext;
import org.jumpmind.symmetric4.job.AbstractScheduledWorker;
import org.jumpmind.symmetric4.job.IScheduler;

public class ReadDataForBatchingWorker extends AbstractScheduledWorker {

    CreateBatchesWorker createBatchesWorker;

    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        super.init(serviceContext, scheduler);
        this.createBatchesWorker = serviceContext.getWorker(CreateBatchesWorker.class);
    }

    @Override
    public String getJobName() {
        return "Read Data For Batching";
    }

    @Override
    protected void run() {
        // detect gaps

        // for eachchannel
        {
            // foreach data
            {
                createBatchesWorker.queue(new Data());
            }
        }
    }

}
