package org.jumpmind.symmetric4.service;

import org.jumpmind.db.platform.IDatabasePlatform;

public class SqliteDialectService extends DialectService {

    public SqliteDialectService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }

}
