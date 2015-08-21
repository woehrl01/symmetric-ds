package org.jumpmind.symmetric4.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.extension.IExtensionPoint;
import org.jumpmind.symmetric.ext.ExtensionPointMetaData;
import org.jumpmind.symmetric4.model.Extension;
import org.jumpmind.util.KeyedCache.ICacheRefresher;

public class ExtensionService extends AbstractCachedService {
    
    protected Map<Class, Map<String, IExtensionPoint>> staticExtensionsByClassByName = new HashMap<Class, Map<String, IExtensionPoint>>();
    
    protected Map<Class, Map<String, IExtensionPoint>> extensionsByClassByName;
    
    protected List<ExtensionPointMetaData> extensionMetaData;    

    public ExtensionService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
    }

    @Override
    protected Class<?>[] getCachedTypes() {
        return new Class<?>[] { Extension.class };
    }
    
    @Override
    protected ICacheRefresher<Class<?>, TreeMap<String, Object>> createCacheRefresher() {
        return new DefaultCacheRefresher() {
          @Override
            public LinkedHashMap<Class<?>, TreeMap<String, Object>> refresh() {
              LinkedHashMap<Class<?>, TreeMap<String, Object>> cache = super.refresh();
              
              
              
              return cache;
            }  
        };
    }

}
