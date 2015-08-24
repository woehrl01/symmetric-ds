package org.jumpmind.symmetric4.service;

import org.jumpmind.db.platform.IDatabasePlatform;

public class JobService extends AbstractSqlService {

    public JobService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }

}
