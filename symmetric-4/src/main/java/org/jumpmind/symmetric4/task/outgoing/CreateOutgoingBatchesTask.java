package org.jumpmind.symmetric4.task.outgoing;

import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric4.task.AbstractSingleThreadedTask;

public class CreateOutgoingBatchesTask extends AbstractSingleThreadedTask<CsvData> {
    
    // It is ok to keep state because this is a "single" queue worker
    
    @Override
    public String getName() {
        return "Create Batches Job";
    }
    
    @Override
    protected void doWork(CsvData work) {
    }

}
