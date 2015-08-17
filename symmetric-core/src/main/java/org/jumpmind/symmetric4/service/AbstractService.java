package org.jumpmind.symmetric4.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.SqlPersistenceManager;
import org.jumpmind.symmetric.common.TableConstants;
import org.jumpmind.symmetric.service.impl.ExtensionService;
import org.jumpmind.util.FormatUtils;
import org.jumpmind.util.KeyedCache;
import org.jumpmind.util.KeyedCache.ICacheRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public abstract class AbstractService {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ISqlTemplate sqlTemplate;

    protected IDatabasePlatform platform;
    
    protected SqlPersistenceManager persistenceManager;

    protected Map<String, String> sqls = new HashMap<String, String>();
    
    protected String tablePrefix;
    
    protected KeyedCache<Class<?>, TreeMap<String, Object>> cache;

    public AbstractService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        this.tablePrefix = tablePrefix;
        this.platform = platform;
        this.sqlTemplate = platform.getSqlTemplate();
        this.persistenceManager = new SqlPersistenceManager(platform);
        this.cache = new KeyedCache<Class<?>, TreeMap<String, Object>>(cacheTimeout, createCacheRefresher());
        initSqls();
        this.cache.refreshCacheIfNeeded(false);
    }
    
    protected abstract Class<?>[] getCachedTypes();
    
    protected ICacheRefresher<Class<?>, TreeMap<String, Object>> createCacheRefresher() {
        return new DefaultCacheRefresher();
    }
    
    protected String getTableNameFor(Object object) {
        String name = null;
        Class<?> type = null;
        if (object instanceof Class) {
            type = (Class<?>) object;
        } else if (object != null) {
            type = object.getClass();
        }
        if (type != null) {
            name = String.format("%s_%s", type.getSimpleName().toLowerCase());
        }
        return name;
    }    
    
    public void save(Object object) {
        persistenceManager.save(object, null, null, getTableNameFor(object));
        cache.find(object.getClass(), false).put(object.toString(), object);
    }
    
    public void update(Object object) {
        persistenceManager.update(object, null, null, getTableNameFor(object));
        cache.find(object.getClass(), false).put(object.toString(), object);
    }

    
    public void insert(Object object) {
        persistenceManager.insert(object, null, null, getTableNameFor(object));
        cache.find(object.getClass(), false).put(object.toString(), object);
    }
    
    public void delete(Object object) {
        persistenceManager.delete(object, null, null, getTableNameFor(object));
        cache.find(object.getClass(), false).remove(object.toString());
    }
    
    public <T> T find (String id, Class<T> clazz) {
        return (T)cache.find(clazz, false).get(id);
    }
    
    public <T> List<T> findAll(Class<T> clazz) {        
        return new ArrayList<T>((Collection<T>)cache.find(clazz, false).values());
    }

    protected void initSqls() {
        InputStream is = getClass().getResourceAsStream(String.format("%s.sql", getClass().getSimpleName()));
        if (is != null) {
            Map<String, String> preScrubbedSqls = (Map<String, String>) new Yaml().load(is);
            Set<String> keys = preScrubbedSqls.keySet();
            for (String key : keys) {
                String statement = preScrubbedSqls.get(key);
                Map<String, String> replacementTokens = createSqlReplacementTokens(tablePrefix,
                        platform.getDatabaseInfo().getDelimiterToken());
                if (replacementTokens != null) {
                    statement = FormatUtils.replaceTokens(statement, replacementTokens, true);
                }
                statement = statement.replaceAll("\\s+", " ");
                this.sqls.put(key, this.platform != null ? this.platform.scrubSql(statement) : statement);
            }
        }

    }

    protected Map<String, String> createSqlReplacementTokens(String tablePrefix, String quotedIdentifier) {
        Map<String, String> map = new HashMap<String, String>();
        List<String> tables = TableConstants.getTablesWithoutPrefix();
        for (String table : tables) {
            map.put(table, String.format("%s%s%s", tablePrefix, StringUtils.isNotBlank(tablePrefix) ? "_" : "", table));
        }
        return map;
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
