package org.jumpmind.symmetric4.task.outgoing;

import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric4.IApplicationContext;
import org.jumpmind.symmetric4.task.AbstractScheduledTask;
import org.jumpmind.symmetric4.task.IScheduler;

public class ChangeDataCaptureReaderTask extends AbstractScheduledTask {

    CreateOutgoingBatchesTask createBatchesWorker;

    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        super.init(serviceContext, scheduler);
        this.createBatchesWorker = serviceContext.getWorker(CreateOutgoingBatchesTask.class);
    }

    @Override
    public String getName() {
        return "Read Data For Batching";
    }

    @Override
    protected void run() {
        // detect gaps

        // for eachchannel
        {
            // foreach data
            {
                createBatchesWorker.queueWork(new CsvData());
            }
        }
    }

}
