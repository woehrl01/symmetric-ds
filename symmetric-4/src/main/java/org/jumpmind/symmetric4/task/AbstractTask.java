package org.jumpmind.symmetric4.task;

import org.jumpmind.symmetric4.IApplicationContext;
import org.jumpmind.symmetric4.service.ConfigurationService;

abstract public class AbstractTask implements ITask {

    protected IApplicationContext appContext;
    
    protected IScheduler scheduler;

    public AbstractTask() {
    }

    @Override
    public void init(IApplicationContext serviceContext, IScheduler scheduler) {
        this.appContext = serviceContext;
        this.scheduler = scheduler;
    }

    protected int getMaxNumberOfThreads() {
        return appContext.getService(ConfigurationService.class).getParameters()
                .getInt(String.format("work.%s.max.number.of.threads", getParameterJobName()));
    }
    
    protected String getParameterJobName() {
        return getName().toLowerCase().replaceAll(" ", "-");
    }    

}
