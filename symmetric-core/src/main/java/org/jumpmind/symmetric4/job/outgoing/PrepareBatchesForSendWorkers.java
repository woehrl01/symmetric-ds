package org.jumpmind.symmetric4.job.outgoing;

import java.util.List;

import org.jumpmind.symmetric4.service.DataExtractorService;
import org.jumpmind.symmetric4.service.StagingService;
import org.jumpmind.symmetric4.IApplicationContext;
import org.jumpmind.symmetric4.job.AbstractScheduledWorkers;
import org.jumpmind.symmetric4.job.IScheduler;
import org.jumpmind.symmetric4.model.NodeChannelBatches;
import org.jumpmind.symmetric4.model.OutgoingBatch;

public class PrepareBatchesForSendWorkers extends AbstractScheduledWorkers<NodeChannelBatches> {

    SendBatchesWorkers sendBatchesJob;

    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        super.init(serviceContext, scheduler);
    }

    @Override
    public String getJobName() {
        return "Prepare Batches For Send";
    }

    @Override
    protected List<NodeChannelBatches> getWork() {

        // use service to look up batches for nodes that need to be send

        // should we have a status that indicates whether a batch has been
        // extracted or not?

        // figure out which nodes that have batches pending should be serviced

        return null;
    }

    @Override
    protected void work(NodeChannelBatches nodeChannelBatches) {
        try {
            if (sendBatchesJob.reserve(nodeChannelBatches)) {
                DataExtractorService service = appContext.getService(DataExtractorService.class);
                StagingService stagingService = appContext.getService(StagingService.class);
                List<OutgoingBatch> batches = nodeChannelBatches.getBatches();
                for (OutgoingBatch outgoingBatch : batches) {                    
                    service.extract(outgoingBatch,stagingService.getWriterFor(outgoingBatch));
                    sendBatchesJob.queue(nodeChannelBatches, outgoingBatch);
                }
            }
        } finally {
            sendBatchesJob.free(nodeChannelBatches);
        }
    }
}