package org.jumpmind.symmetric4.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.SqlPersistenceManager;
import org.jumpmind.symmetric4.model.TableConstants;
import org.jumpmind.util.FormatUtils;
import org.jumpmind.util.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class AbstractSqlService implements IService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ISqlTemplate sqlTemplate;

    protected IDatabasePlatform platform;

    protected SqlPersistenceManager persistenceManager;

    protected Map<String, String> sqlCache = new HashMap<String, String>();

    protected String tablePrefix;

    public AbstractSqlService(String tablePrefix, long cacheTimeout, IDatabasePlatform platform) {
        this.tablePrefix = tablePrefix;
        this.platform = platform;
        this.sqlTemplate = platform.getSqlTemplate();
        this.persistenceManager = new SqlPersistenceManager(platform);
        initSqlCache();
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
    }

    public void update(Object object) {
        persistenceManager.update(object, null, null, getTableNameFor(object));
    }

    public void insert(Object object) {
        persistenceManager.insert(object, null, null, getTableNameFor(object));
    }

    public void delete(Object object) {
        persistenceManager.delete(object, null, null, getTableNameFor(object));
    }

    protected void initSqlCache() {
        List<String> names = new ArrayList<String>();
        Class<?> me = getClass();
        do {
            names.add(0, me.getSimpleName());
            me = me.getSuperclass();
        } while (me != null);

        for (String name : names) {
            /*
             * Read from root classpath first. This allows an override from the
             * root of the patches directory
             */
            InputStream is = getClass().getResourceAsStream(String.format("/%s.sql", name));
            if (is == null) {
                is = getClass().getResourceAsStream(String.format("%s.sql", name));
            }
            
            if (is != null) {
                @SuppressWarnings("unchecked")
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
                    this.sqlCache.put(key, this.platform != null ? this.platform.scrubSql(statement) : statement);
                }
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

}
