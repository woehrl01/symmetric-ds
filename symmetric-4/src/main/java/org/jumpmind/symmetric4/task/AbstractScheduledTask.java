package org.jumpmind.symmetric4.task;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.jumpmind.symmetric4.service.ConfigurationService;

abstract public class AbstractScheduledTask extends AbstractTask {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
    
    public void scheduleNow() {
        
    }
    
    @Override
    protected int getMaxNumberOfThreads() {
        return 1;
    }

    protected String getScheduleExpression() {
        return isNotBlank(getCronExpression()) ? getCronExpression() : getPeriodExpression();
    }

    protected String getCronExpression() {
        return appContext.getService(ConfigurationService.class).getParameters()
                .get(String.format("job.%s.cron", getName().toLowerCase()));
    }

    protected String getPeriodExpression() {
        return appContext.getService(ConfigurationService.class).getParameters()
                .get(String.format("job.%s.period.ms", getName().toLowerCase()));
    }

    abstract protected void run();

}
