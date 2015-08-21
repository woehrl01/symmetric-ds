package org.jumpmind.symmetric4.job.outgoing;

import org.jumpmind.symmetric4.job.AbstractReservedQueueWorkers;
import org.jumpmind.symmetric4.model.NodeChannelBatches;
import org.jumpmind.symmetric4.model.OutgoingBatch;

public class SendBatchesWorkers extends AbstractReservedQueueWorkers<NodeChannelBatches, OutgoingBatch> {

    @Override
    public String getJobName() {
        return "Send Batches";
    }
    
    @Override
    protected void work(NodeChannelBatches reservationToken, OutgoingBatch work) {
        
    }



}