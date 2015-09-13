package org.jumpmind.symmetric4.task.outgoing;

import org.jumpmind.symmetric4.model.NodeChannelBatches;
import org.jumpmind.symmetric4.model.OutgoingBatch;
import org.jumpmind.symmetric4.task.AbstractReservedThreadTask;

public class SendOutgoingBatchesTask extends AbstractReservedThreadTask<NodeChannelBatches, OutgoingBatch> {

    @Override
    public String getName() {
        return "Send Batches";
    }
    
    @Override
    protected void doWork(NodeChannelBatches reservationToken, OutgoingBatch work) {
        
    }



}