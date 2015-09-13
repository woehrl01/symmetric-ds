package org.jumpmind.symmetric4.task.manage;

import org.jumpmind.symmetric4.service.DialectService;
import org.jumpmind.symmetric4.task.AbstractScheduledTask;

public class SyncTriggersTask extends AbstractScheduledTask {

    public SyncTriggersTask() {
    }
    
    @Override
    public String getName() {
        return "Sync Trigger";
    }
    
    @Override
    protected void run() {
        appContext.getService(DialectService.class).syncTriggers(false);
    }

}
