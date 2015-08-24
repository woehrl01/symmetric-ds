package org.jumpmind.symmetric4.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.jumpmind.symmetric.model.IncomingBatch;
import org.jumpmind.symmetric4.model.OutgoingBatch;

// should we be using java nio?
public class StagingService {

    public StagingService() {
    }
    
    public Writer getWriter(OutgoingBatch outgoingBatch) {
        return null;
    }
    
    public OutputStream getOutputStream(OutgoingBatch outgoingBatch) {
        return null;
    }

    
    public Reader getReader(IncomingBatch incomingBatch) {
        return null;
    }
    
    public InputStream getInputStream(IncomingBatch incomingBatch) {
        return null;
    }


}
