package org.jumpmind.symmetric4.job.manage;

import org.jumpmind.symmetric4.job.AbstractScheduledWorker;
import org.jumpmind.symmetric4.service.DialectService;

public class SyncTriggersWorker extends AbstractScheduledWorker {

    public SyncTriggersWorker() {
    }
    
    @Override
    public String getJobName() {
        return "Sync Trigger";
    }
    
    @Override
    protected void run() {
        appContext.getService(DialectService.class).syncTriggers(false);
    }

}
