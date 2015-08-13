package org.jumpmind.symmetric4.model;

import java.util.Date;

public class JobStatus extends AbstractObject {

    public static final String STOPPED = "STOPPED";
    
    private static final long serialVersionUID = 1L;

    private String jobInstance;
    private String jobName;
    private String lockingServerId;
    private Date lockTime;
    private int sharedCount;
    private boolean sharedEnable;
    private Date lastLockTime;
    private String lastLockingServerId;

    public String getJobInstance() {
        return jobInstance;
    }
    
    public boolean isStopped() {
       return STOPPED.equals(lockingServerId) && lockTime != null;
    }
    
    public boolean isLockedByOther(String serverId) {
        return lockTime != null  && lockingServerId != null && !lockingServerId.equals(serverId);
    }

    public void setJobInstance(String lockAction) {
        this.jobInstance = lockAction;
    }

    public String getLockingServerId() {
        return lockingServerId;
    }

    public void setLockingServerId(String lockingServerId) {
        this.lockingServerId = lockingServerId;
    }

    public Date getLockTime() {
        return lockTime;
    }

    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    public Date getLastLockTime() {
        return lastLockTime;
    }

    public void setLastLockTime(Date lastLockTime) {
        this.lastLockTime = lastLockTime;
    }

    public String getLastLockingServerId() {
        return lastLockingServerId;
    }

    public void setLastLockingServerId(String lastLockingServerId) {
        this.lastLockingServerId = lastLockingServerId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String lockType) {
        this.jobName = lockType;
    }

    public int getSharedCount() {
        return sharedCount;
    }

    public void setSharedCount(int sharedCount) {
        this.sharedCount = sharedCount;
    }

    public boolean isSharedEnable() {
        return sharedEnable;
    }

    public void setSharedEnable(boolean sharedEnable) {
        this.sharedEnable = sharedEnable;
    }


}
