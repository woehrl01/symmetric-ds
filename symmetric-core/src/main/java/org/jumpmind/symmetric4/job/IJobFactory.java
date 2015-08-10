package org.jumpmind.symmetric4.job;

import java.util.List;

import org.jumpmind.extension.IBuiltInExtensionPoint;

public interface IJobFactory<T> extends IBuiltInExtensionPoint {

    public String getJobName();
    
    public ScheduleType getScheduleType();
    
    public String getScheduleExpression();
    
    public int getMaxNumberOfWorkersPerRun();
    
    public IJob<T> create();
    
    public List<T> getInstancesForRun();
    
}
