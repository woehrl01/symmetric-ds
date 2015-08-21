package org.jumpmind.symmetric4.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.util.KeyedCache;
import org.jumpmind.util.KeyedCache.ICacheRefresher;

@SuppressWarnings("unchecked")
public abstract class AbstractCachedService extends AbstractSqlService {
    
    protected KeyedCache<Class<?>, TreeMap<String, Object>> cache;

    public AbstractCachedService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
        this.cache = new KeyedCache<Class<?>, TreeMap<String, Object>>(cacheTimeout, createCacheRefresher());
        this.cache.refreshCacheIfNeeded(false);
    }
    
    protected abstract Class<?>[] getCachedTypes();
    
    protected ICacheRefresher<Class<?>, TreeMap<String, Object>> createCacheRefresher() {
        return new DefaultCacheRefresher();
    }
    
    public void save(Object object) {
        super.save(object);
        cache.find(object.getClass(), false).put(object.toString(), object);
    }
    
    public void update(Object object) {
        super.update(object);
        cache.find(object.getClass(), false).put(object.toString(), object);
    }

    
    public void insert(Object object) {
        super.insert(object);
        cache.find(object.getClass(), false).put(object.toString(), object);
    }
    
    public void delete(Object object) {
        super.delete(object);
        cache.find(object.getClass(), false).remove(object.toString());
    }
    
    public <T> T find (String id, Class<T> clazz) {
        return (T)cache.find(clazz, false).get(id);
    }
    
    public <T> List<T> findAll(Class<T> clazz) {        
        return new ArrayList<T>((Collection<T>)cache.find(clazz, false).values());
    }
    
    public class DefaultCacheRefresher implements ICacheRefresher<Class<?>, TreeMap<String, Object>> {
        @Override
        public LinkedHashMap<Class<?>, TreeMap<String, Object>> refresh() {
            Class<?>[] types = getCachedTypes();
            LinkedHashMap<Class<?>, TreeMap<String, Object>> map = new LinkedHashMap<Class<?>, TreeMap<String, Object>>(types.length);
            for (Class<?> type : types) {
                List<?> all = persistenceManager.find(type, null, null, getTableNameFor(type));
                TreeMap<String, Object> objectMap = new TreeMap<String, Object>();
                for (Object object : all) {
                    objectMap.put(object.toString(), object);
                }
                map.put(type, objectMap);                    
            }
            return map;
        }
    }

}
