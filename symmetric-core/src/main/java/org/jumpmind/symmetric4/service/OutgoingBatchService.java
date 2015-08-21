package org.jumpmind.symmetric4.service;

import org.jumpmind.db.platform.IDatabasePlatform;

public class OutgoingBatchService extends AbstractSqlService {

    public OutgoingBatchService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }

}
