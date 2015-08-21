package org.jumpmind.symmetric4.job;

import org.jumpmind.extension.IExtensionPoint;
import org.jumpmind.symmetric4.IApplicationContext;

public interface IWorker extends IExtensionPoint {

    public void init(IApplicationContext serviceContext, IScheduler scheduler);
    
    public void start();
    
    public void stop();    
    
    public String getJobName();    
        
}
