package org.jumpmind.symmetric4.model;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.io.data.DataEventType;

final public class TriggerRouterUtils {

    private TriggerRouterUtils() {
    }
    
    public static boolean isRouted(Router router, DataEventType event) {
        switch (event) {
            case INSERT:
                return router.isSyncOnInsert();
            case DELETE:
                return router.isSyncOnDelete();
            case UPDATE:
                return router.isSyncOnUpdate();
            default:
                return true;
        }
    }

    public static String getTargetSchema(Router router, String defaultSchema) {
        if (router != null && !StringUtils.isBlank(router.getTargetSchemaName())) {
            return router.getTargetSchemaName();
        } else {
            return defaultSchema;
        }
    }

    public static String getTargetCatalog(Router router, String defaultCatalog) {
        if (router != null && !StringUtils.isBlank(router.getTargetCatalogName())) {
            return router.getTargetCatalogName();
        } else {
            return defaultCatalog;
        }
    }

    public static String getTargetTable(Router router, Trigger trigger, TriggerHistory triggerHistory) {
        if (router != null && !StringUtils.isBlank(router.getTargetTableName())) {
            return router.getTargetTableName();
        }
        if (triggerHistory != null) {
            return triggerHistory.getSourceTableName();
        }
        if (trigger != null && !StringUtils.isBlank(trigger.getSourceTableName())) {
            return trigger.getSourceTableName();
        } else {
            return null;
        }
    }

    public static String qualifiedTargetTableName(Router router, Trigger trigger, TriggerHistory triggerHistory) {
        String catalog = router.getTargetCatalogName();
        String schema = router.getTargetSchemaName();
        String tableName = getTargetTable(router, trigger, triggerHistory);
        if (!StringUtils.isBlank(schema)) {
            tableName = schema + "." + tableName;
        }
        if (!StringUtils.isBlank(catalog)) {
            tableName = catalog + "." + tableName;
        }
        return tableName;
    }

}
