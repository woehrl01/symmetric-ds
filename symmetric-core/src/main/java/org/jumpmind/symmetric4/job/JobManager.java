package org.jumpmind.symmetric4.job;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jumpmind.symmetric4.service.ConfigurationService;

public class JobManager {

    Map<String, ExecutorService> executors;
    
    Map<Object, IJob<?>> runningJobs;    
    
    ConfigurationService configurationService;
    
    public JobManager(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    
    public void register(IJobFactory<?> factory) {
        
    }
    
    public void start() {
        
    }
    
    public void restart(String jobName) {
        
    }
    
    public void stop() {
        
    }    
    
}
