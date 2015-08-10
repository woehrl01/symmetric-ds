package org.jumpmind.symmetric4.job;

import java.util.List;

import org.jumpmind.extension.IExtensionPoint;

public interface IJob<T> extends IExtensionPoint {

    public void run(T t);
    
    public List<T> getInstancesForRun();    
    
}
