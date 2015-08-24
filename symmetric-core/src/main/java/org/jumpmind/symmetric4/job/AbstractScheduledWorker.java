package org.jumpmind.symmetric4.job;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.jumpmind.symmetric4.service.ConfigurationService;

abstract public class AbstractScheduledWorker extends AbstractWorker {

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
                .get(String.format("job.%s.cron", getJobName().toLowerCase()));
    }

    protected String getPeriodExpression() {
        return appContext.getService(ConfigurationService.class).getParameters()
                .get(String.format("job.%s.period.ms", getJobName().toLowerCase()));
    }

    abstract protected void run();

}
