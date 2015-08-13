package org.jumpmind.symmetric4.model;

import java.io.Serializable;
import java.util.Date;

abstract public class AbstractObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Date createTime;

    private Date lastUpdateTime;

    private String lastUpdateBy;
    
    public AbstractObject() {
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setLastUpdateBy(String lastUpdateBy) {
        this.lastUpdateBy = lastUpdateBy;
    }
    
    public String getLastUpdateBy() {
        return lastUpdateBy;
    }
    
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

}
