package org.jumpmind.symmetric4.job;

import java.util.Map;
import java.util.concurrent.ExecutorService;

public class JobManager {

    Map<String, ExecutorService> executors;
    
    Map<Object, IJob<?>> runningJobs;    
    
    public void register(IJobFactory<?> factory) {
        
    }
    
    public void start() {
        
    }
    
    public void restart(String jobName) {
        
    }
    
    public void stop() {
        
    }    
    
}
