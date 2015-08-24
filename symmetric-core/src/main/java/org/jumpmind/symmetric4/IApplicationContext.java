package org.jumpmind.symmetric4;

public interface IApplicationContext {

    <T> T getService(Class<T> clazz);
    
    <T> T getWorker(Class<T> clazz);
    
}
