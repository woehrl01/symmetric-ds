package org.jumpmind.symmetric4.job;

import org.jumpmind.symmetric4.IApplicationContext;
import org.jumpmind.symmetric4.service.ConfigurationService;

abstract public class AbstractWorker implements IWorker {

    protected IApplicationContext appContext;
    
    protected IScheduler scheduler;

    public AbstractWorker() {
    }

    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        this.appContext = serviceContext;
        this.scheduler = scheduler;
    }

    protected int getMaxNumberOfThreads() {
        return appContext.getService(ConfigurationService.class).getParameters()
                .getInt(String.format("job.%s.max.number.of.threads", getJobName().toLowerCase()));
    }
    
    protected String getParameterJobName() {
        return getJobName().toLowerCase().replaceAll(" ", "-");
    }    

}
