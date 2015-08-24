package org.jumpmind.symmetric4.service;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric4.model.TriggerHistory;

abstract public class DialectService extends AbstractCachedService {

    public DialectService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }

    @Override
    protected Class<?>[] getCachedTypes() {
        return new Class<?>[] { TriggerHistory.class };
    }
    
    public void verifyCompatibilty() {
        
    }
    
    public void initializeDatabase() {        
    }
    
    public void uninstallDatabase() {        
    }

    public boolean syncTriggers(boolean force) {
        return false;
    }

    public boolean syncTrigger(String triggerId, boolean force) {
        return false;
    }

}
