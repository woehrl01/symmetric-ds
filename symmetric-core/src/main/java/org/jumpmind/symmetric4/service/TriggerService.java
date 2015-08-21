package org.jumpmind.symmetric4.service;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric4.model.TriggerHistory;

public class TriggerService extends AbstractCachedService {

    
    public TriggerService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }

    @Override
    protected Class<?>[] getCachedTypes() {
        return new Class<?>[] { TriggerHistory.class };
    }

}
