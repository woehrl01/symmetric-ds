package org.jumpmind.symmetric4.service;

import java.io.Writer;

import org.jumpmind.symmetric4.model.OutgoingBatch;

public class StagingService {

    public StagingService() {
    }
    
    public Writer getWriterFor(OutgoingBatch outgoingBatch) {
        // should we be using java nio?
        return null;
    }

}
