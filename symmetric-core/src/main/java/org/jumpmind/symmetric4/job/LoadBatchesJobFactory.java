package org.jumpmind.symmetric4.job;

import org.jumpmind.symmetric4.model.Channel;

public class LoadBatchesJobFactory implements IJobFactory<Channel> {

    public LoadBatchesJobFactory() {
    }
    
    @Override
    public IJob<Channel> create() {
        return null;
    }
    
    @Override
    public java.util.List<Channel> getInstancesForRun() {
        return null;
    };
    
    @Override
    public String getJobName() {
        return null;
    }
    
    @Override
    public int getMaxNumberOfWorkersPerRun() {
        return 0;
    }

    @Override
    public String getScheduleExpression() {
        return null;
    }
    
    @Override
    public ScheduleType getScheduleType() {
        return null;
    }

}
