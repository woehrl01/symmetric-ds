package org.jumpmind.symmetric4.service;

import java.io.Writer;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric4.model.OutgoingBatch;

public class DataExtractorService extends AbstractSqlService {

    public DataExtractorService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }
    
    public void extract(OutgoingBatch batch, Writer writer) {
        
    }

}
