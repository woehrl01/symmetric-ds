/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */

package org.jumpmind.symmetric.service.impl;

import java.sql.DataTruncation;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.ISqlReadCursor;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.UniqueKeyException;
import org.jumpmind.db.sql.mapper.NumberMapper;
import org.jumpmind.symmetric.ISymmetricEngine;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.TableConstants;
import org.jumpmind.symmetric.db.SequenceIdentifier;
import org.jumpmind.symmetric.ext.IHeartbeatListener;
import org.jumpmind.symmetric.io.data.Batch;
import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric.io.data.CsvUtils;
import org.jumpmind.symmetric.io.data.DataEventType;
import org.jumpmind.symmetric.job.PushHeartbeatListener;
import org.jumpmind.symmetric.load.IReloadListener;
import org.jumpmind.symmetric.model.Data;
import org.jumpmind.symmetric.model.DataEvent;
import org.jumpmind.symmetric.model.DataGap;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.NodeGroupLink;
import org.jumpmind.symmetric.model.NodeGroupLinkAction;
import org.jumpmind.symmetric.model.NodeSecurity;
import org.jumpmind.symmetric.model.OutgoingBatch;
import org.jumpmind.symmetric.model.OutgoingBatch.Status;
import org.jumpmind.symmetric.model.Router;
import org.jumpmind.symmetric.model.TableReloadRequest;
import org.jumpmind.symmetric.model.TableReloadRequestKey;
import org.jumpmind.symmetric.model.Trigger;
import org.jumpmind.symmetric.model.TriggerHistory;
import org.jumpmind.symmetric.model.TriggerRouter;
import org.jumpmind.symmetric.service.IDataService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.ITriggerRouterService;
import org.jumpmind.util.AppUtils;

/**
 * @see IDataService
 */
public class DataService extends AbstractService implements IDataService {

    private ISymmetricEngine engine;

    private List<IReloadListener> reloadListeners;

    private List<IHeartbeatListener> heartbeatListeners;

    private DataMapper dataMapper;

    public DataService(ISymmetricEngine engine) {
        super(engine.getParameterService(), engine.getSymmetricDialect());
        this.engine = engine;
        this.reloadListeners = new ArrayList<IReloadListener>();
        this.heartbeatListeners = new ArrayList<IHeartbeatListener>();
        this.heartbeatListeners.add(new PushHeartbeatListener(engine));
        this.dataMapper = new DataMapper();

        setSqlMap(new DataServiceSqlMap(symmetricDialect.getPlatform(),
                createSqlReplacementTokens()));
    }

    protected Map<IHeartbeatListener, Long> lastHeartbeatTimestamps = new HashMap<IHeartbeatListener, Long>();
    
    public boolean insertReloadEvent(TableReloadRequest request) {
        return insertReloadEvent(request, true);
    }
    
    public boolean insertReloadEvent(TableReloadRequest request, boolean updateTableReloadRequest) {
        boolean successful = false;
        if (request.isReloadEnabled()) {
            ITriggerRouterService triggerRouterService = engine.getTriggerRouterService();
            INodeService nodeService = engine.getNodeService();
            Node targetNode = nodeService.findNode(request.getTargetNodeId());
            if (targetNode != null) {
                TriggerRouter triggerRouter = triggerRouterService.
                        getTriggerRouterForCurrentNode(request.getTriggerId(), request.getRouterId(), false);
                if (triggerRouter != null) {
                    Trigger trigger = triggerRouter.getTrigger();
                    Router router = triggerRouter.getRouter();

                    NodeGroupLink link = router.getNodeGroupLink();
                    Node me = nodeService.findIdentity();
                    if (link.getSourceNodeGroupId().equals(me.getNodeGroupId())) {
                        if (link.getTargetNodeGroupId().equals(targetNode.getNodeGroupId())) {

                            TriggerHistory triggerHistory = lookupTriggerHistory(trigger);

                            ISqlTransaction transaction = null;
                            try {
                                transaction = sqlTemplate.startSqlTransaction();

                                String deleteStatement = StringUtils.isNotBlank(request
                                        .getReloadDeleteStmt()) ? request.getReloadDeleteStmt()
                                        : triggerRouter.getInitialLoadDeleteStmt();
                                if (StringUtils.isNotBlank(deleteStatement)) {
                                    insertPurgeEvent(transaction, targetNode, triggerRouter,
                                            triggerHistory, false, request.getReloadDeleteStmt(), -1, null);
                                }

                                insertReloadEvent(transaction, targetNode, triggerRouter,
                                        triggerHistory, request.getReloadSelect(), false, -1, null);

                                if (updateTableReloadRequest) {
                                    insertSqlEvent(
                                            transaction,
                                            triggerHistory,
                                            trigger.getChannelId(),
                                            targetNode,
                                            String.format(
                                                    "update %s set reload_enabled=0, reload_time=current_timestamp where target_node_id='%s' and source_node_id='%s' and trigger_id='%s' and router_id='%s'",
                                                    TableConstants
                                                            .getTableName(
                                                                    tablePrefix,
                                                                    TableConstants.SYM_TABLE_RELOAD_REQUEST),
                                                    request.getTargetNodeId(), request
                                                            .getSourceNodeId(), request
                                                            .getTriggerId(), request.getRouterId()),
                                            false, -1, null);
                                }
                                
                                transaction.commit();
                                
                                request.setReloadEnabled(false);
                                request.setReloadTime(new Date());
                                request.setLastUpdateBy("symmetricds");
                                if (updateTableReloadRequest) {
                                    saveTableReloadRequest(request);
                                }

                            } finally {
                                close(transaction);
                            }

                        } else {
                            log.error(
                                    "Could not reload table for node {} because the router {} target node group id {} did not match",
                                    new Object[] { request.getTargetNodeId(),
                                            request.getRouterId(), link.getTargetNodeGroupId() });
                        }
                    } else {
                        log.error(
                                "Could not reload table for node {} because the router {} source node group id {} did not match",
                                new Object[] { request.getTargetNodeId(), request.getRouterId(),
                                        link.getSourceNodeGroupId() });
                    }
                } else {
                    log.error(
                            "Could not reload table for node {} because the trigger router ({}, {}) could not be found",
                            new Object[] { request.getTargetNodeId(), request.getTriggerId(),
                                    request.getRouterId() });
                }
            } else {
                log.error("Could not reload table for node {} because the node could not be found",
                        request.getTargetNodeId());
            }
        }
        return successful;

    }
    
    public void saveTableReloadRequest(TableReloadRequest request) {
        Date time = new Date();
        request.setLastUpdateTime(time);
        if (0 == sqlTemplate.update(
                getSql("updateTableReloadRequest"),
                new Object[] { request.getReloadSelect(), request.getReloadDeleteStmt(),
                        request.isReloadEnabled() ? 1 : 0, request.getReloadTime(),
                        request.getCreateTime(), request.getLastUpdateBy(),
                        request.getLastUpdateTime(), request.getSourceNodeId(),
                        request.getTargetNodeId(), request.getTriggerId(), request.getRouterId() },
                new int[] { Types.VARCHAR, Types.VARCHAR, Types.SMALLINT, Types.TIMESTAMP,
                        Types.TIMESTAMP, Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR })) {
            request.setCreateTime(time);
            sqlTemplate.update(
                    getSql("insertTableReloadRequest"),
                    new Object[] { request.getReloadSelect(), request.getReloadDeleteStmt(),
                            request.isReloadEnabled() ? 1 : 0, request.getReloadTime(),
                            request.getCreateTime(), request.getLastUpdateBy(),
                            request.getLastUpdateTime(), request.getSourceNodeId(),
                            request.getTargetNodeId(),
                            request.getTriggerId(),  request.getRouterId() }, new int[] { Types.VARCHAR, Types.VARCHAR,
                            Types.SMALLINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR,
                            Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                            Types.VARCHAR });
        }
    }
    
    public TableReloadRequest getTableReloadRequest(final TableReloadRequestKey key) {
        return sqlTemplate.queryForObject(getSql("selectTableReloadRequest"), new ISqlRowMapper<TableReloadRequest>() {
            public TableReloadRequest mapRow(Row rs) {
                TableReloadRequest request = new TableReloadRequest(key);       
                request.setReloadSelect(rs.getString("reload_select"));
                request.setReloadEnabled(rs.getBoolean("reload_enabled"));
                request.setReloadTime(rs.getDateTime("reload_time"));
                request.setCreateTime(rs.getDateTime("create_time"));
                request.setLastUpdateBy(rs.getString("last_update_by"));
                request.setLastUpdateTime(rs.getDateTime("last_update_time"));
                return request;
            }
        }, key.getSourceNodeId(), key.getTargetNodeId(), key.getTriggerId(), key.getRouterId());
    }

    protected void insertReloadEvent(final Node targetNode, final TriggerRouter triggerRouter) {
        insertReloadEvent(targetNode, triggerRouter, null);
    }

    protected void insertReloadEvent(Node targetNode, TriggerRouter triggerRouter,
            String overrideInitialLoadSelect) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            insertReloadEvent(transaction, targetNode, triggerRouter, null,
                    overrideInitialLoadSelect, -1, null);
            transaction.commit();
        } finally {
            close(transaction);
        }
    }
    
    public void insertReloadEvent(ISqlTransaction transaction, Node targetNode,
            TriggerRouter triggerRouter, TriggerHistory triggerHistory,
            String overrideInitialLoadSelect, long loadId, String createBy) {
        insertReloadEvent(transaction, targetNode, triggerRouter, triggerHistory, overrideInitialLoadSelect, true, loadId, createBy);
    }

    protected void insertReloadEvent(ISqlTransaction transaction, Node targetNode,
            TriggerRouter triggerRouter, TriggerHistory triggerHistory,
            String overrideInitialLoadSelect, boolean isLoad, long loadId, String createBy) {

        if (triggerHistory == null) {
            triggerHistory = lookupTriggerHistory(triggerRouter.getTrigger());
        }

        // initial_load_select for table can be overridden by populating the
        // row_data
        Data data = new Data(triggerHistory.getSourceTableName(), DataEventType.RELOAD,
                overrideInitialLoadSelect != null ? overrideInitialLoadSelect
                        : triggerRouter.getInitialLoadSelect(), null, triggerHistory, isLoad ? Constants.CHANNEL_RELOAD : triggerRouter
                        .getTrigger().getChannelId(), null, null);
        insertDataAndDataEventAndOutgoingBatch(transaction, data, targetNode.getNodeId(),
                triggerRouter.getRouter().getRouterId(), isLoad, loadId, createBy);
    }

    public void insertReloadEvents(Node targetNode, boolean reverse) {
        
        /*
         * Outgoing data events are pointless because we are reloading all data
         */
        engine.getOutgoingBatchService().markAllAsSentForNode(targetNode.getNodeId());

        INodeService nodeService = engine.getNodeService();
        
        Node sourceNode = nodeService.findIdentity();                

        boolean transactional = parameterService
                .is(ParameterConstants.DATA_RELOAD_IS_BATCH_INSERT_TRANSACTIONAL);
        
        boolean useReloadChannel = parameterService.is(ParameterConstants.INITIAL_LOAD_USE_RELOAD_CHANNEL);
        
        String nodeIdRecord = reverse ? nodeService.findIdentityNodeId() : targetNode.getNodeId();
        NodeSecurity nodeSecurity = nodeService.findNodeSecurity(nodeIdRecord);
        
        ISqlTransaction transaction = null;
        
        try {

            long loadId = engine.getSequenceService().nextVal(Constants.SEQUENCE_OUTGOING_BATCH_LOAD_ID);
            String createBy = reverse ? nodeSecurity.getRevInitialLoadCreateBy() : nodeSecurity.getInitialLoadCreateBy();
            
            transaction = platform.getSqlTemplate().startSqlTransaction();

            if (reloadListeners != null) {
                for (IReloadListener listener : reloadListeners) {
                    listener.beforeReload(transaction, targetNode);

                    if (!transactional) {
                        transaction.commit();
                    }
                }
            }

            /*
             * Insert node security so the client doing the initial load knows
             * that an initial load is currently happening
             */
            insertNodeSecurityUpdate(transaction, nodeIdRecord, targetNode.getNodeId(), useReloadChannel, loadId, createBy);
            
            /*
             * Mark incoming batches as OK at the target node because we marked
             * outgoing batches as OK at the source
             */
            insertSqlEvent(
                    transaction,
                    targetNode,
                    String.format(
                            "update %s_incoming_batch set status='OK', error_flag=0 where node_id='%s' and status != 'OK'",
                            tablePrefix, nodeService
                                    .findIdentityNodeId()), true, loadId, createBy);            

            List<TriggerHistory> triggerHistories = engine.getTriggerRouterService()
                    .getActiveTriggerHistories();

            Map<Integer, List<TriggerRouter>> triggerRoutersByHistoryId = fillTriggerRoutersByHistIdAndSortHist(
                    sourceNode, targetNode, triggerHistories);

            if (parameterService.is(ParameterConstants.INITIAL_LOAD_CREATE_SCHEMA_BEFORE_RELOAD)) {
                for (TriggerHistory triggerHistory : triggerHistories) {
                    List<TriggerRouter> triggerRouters = triggerRoutersByHistoryId
                            .get(triggerHistory.getTriggerHistoryId());
                    for (TriggerRouter triggerRouter : triggerRouters) {
                        if (triggerRouter.getInitialLoadOrder() >= 0 && engine.getGroupletService().isTargetEnabled(triggerRouter, targetNode)) {
                            String xml = symmetricDialect.getCreateTableXML(triggerHistory, triggerRouter);
                            insertCreateEvent(transaction, targetNode, triggerRouter, triggerHistory, xml, useReloadChannel, loadId, createBy);
                            if (!transactional) {
                                transaction.commit();
                            }
                        }
                    }
                }
            }

            for (ListIterator<TriggerHistory> triggerHistoryIterator = triggerHistories
                    .listIterator(triggerHistories.size()); triggerHistoryIterator
                    .hasPrevious();) {
                TriggerHistory triggerHistory = triggerHistoryIterator.previous();
                List<TriggerRouter> triggerRouters = triggerRoutersByHistoryId
                        .get(triggerHistory.getTriggerHistoryId());
                for (ListIterator<TriggerRouter> iterator = triggerRouters
                        .listIterator(triggerRouters.size()); iterator.hasPrevious();) {
                    TriggerRouter triggerRouter = iterator.previous();
                    if (triggerRouter.getInitialLoadOrder() >= 0 && 
                            engine.getGroupletService().isTargetEnabled(triggerRouter, targetNode) &&
                    		(parameterService.is(ParameterConstants.INITIAL_LOAD_DELETE_BEFORE_RELOAD) ||
                    				!StringUtils.isEmpty(triggerRouter.getInitialLoadDeleteStmt()))
                    		) {
                        insertPurgeEvent(transaction, targetNode, triggerRouter, triggerHistory, useReloadChannel, null, loadId, createBy);
                        if (!transactional) {
                            transaction.commit();
                        }
                    }
                }
            }

            for (TriggerHistory triggerHistory : triggerHistories) {
                List<TriggerRouter> triggerRouters = triggerRoutersByHistoryId.get(triggerHistory
                        .getTriggerHistoryId());
                for (TriggerRouter triggerRouter : triggerRouters) {
                    if (triggerRouter.getInitialLoadOrder() >= 0  && 
                            engine.getGroupletService().isTargetEnabled(triggerRouter, targetNode)) {
                        insertReloadEvent(transaction, targetNode, triggerRouter, triggerHistory,
                                null, loadId, createBy);
                        if (!transactional) {
                            transaction.commit();
                        }
                    }
                }
            }

            if (reloadListeners != null) {
                for (IReloadListener listener : reloadListeners) {
                    listener.afterReload(transaction, targetNode);
                    if (!transactional) {
                        transaction.commit();
                    }
                }
            }

            if (!reverse) {
                nodeService.setInitialLoadEnabled(transaction, nodeIdRecord, false, false, createBy);
            } else {
                nodeService.setReverseInitialLoadEnabled(transaction, nodeIdRecord, false, false, createBy);
            }
            
            insertNodeSecurityUpdate(transaction, nodeIdRecord, targetNode.getNodeId(),
                    useReloadChannel, loadId, createBy);

            engine.getStatisticManager().incrementNodesLoaded(1);

            transaction.commit();
        } catch (Error ex) {
            transaction.rollback();
            throw ex;
        } catch (RuntimeException ex) {
            transaction.rollback();
            throw ex;            
        } finally {
            close(transaction);
        }

        if (!reverse) {
            /*
             * Remove all incoming events for the node that we are starting a
             * reload for
             */
            engine.getPurgeService().purgeAllIncomingEventsForNode(targetNode.getNodeId());
        }

    }

    private TriggerHistory lookupTriggerHistory(Trigger trigger) {
        TriggerHistory history = engine.getTriggerRouterService().getNewestTriggerHistoryForTrigger(
                trigger.getTriggerId());

        if (history == null) {
            engine.getTriggerRouterService().syncTriggers();
            history = engine.getTriggerRouterService().getNewestTriggerHistoryForTrigger(
                    trigger.getTriggerId());
        }

        if (history == null) {
            throw new RuntimeException("Cannot find history for trigger " + trigger.getTriggerId()
                    + ", " + trigger.getSourceTableName());
        }
        return history;
    }

    public void insertPurgeEvent(Node targetNode, TriggerRouter triggerRouter, TriggerHistory triggerHistory, boolean isLoad, long loadId, String createBy) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            insertPurgeEvent(transaction, targetNode, triggerRouter, triggerHistory, isLoad, null, loadId, createBy);
            transaction.commit();
        } finally {
            close(transaction);
        }
    }

    protected void insertPurgeEvent(ISqlTransaction transaction, Node targetNode,
            TriggerRouter triggerRouter, TriggerHistory triggerHistory, boolean isLoad, String overrideDeleteStatement, long loadId, String createBy) {
        String sql = StringUtils.isNotBlank(overrideDeleteStatement) ? overrideDeleteStatement : symmetricDialect.createPurgeSqlFor(targetNode, triggerRouter, triggerHistory);
        Trigger trigger = triggerRouter.getTrigger();
        Data data = new Data(triggerHistory.getSourceTableName(), DataEventType.SQL,
                CsvUtils.escapeCsvData(sql), null, triggerHistory, isLoad ? Constants.CHANNEL_RELOAD : trigger
                        .getChannelId(), null, null);
        insertDataAndDataEventAndOutgoingBatch(transaction, data, targetNode.getNodeId(),
                triggerRouter.getRouter().getRouterId(), isLoad, loadId, createBy);
    }

    public void insertSqlEvent(Node targetNode, TriggerHistory triggerHistory, String sql,
            boolean isLoad, long loadId, String createBy) {
        Trigger trigger = engine.getTriggerRouterService().getTriggerById(
                triggerHistory.getTriggerId());
        String channelId = Constants.CHANNEL_RELOAD;
        if (trigger != null && !isLoad) {
            channelId = trigger.getChannelId();
        }
        Data data = new Data(triggerHistory.getSourceTableName(), DataEventType.SQL,
                CsvUtils.escapeCsvData(sql), null, triggerHistory, channelId, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(),
                Constants.UNKNOWN_ROUTER_ID, isLoad, loadId, createBy);
    }

    private TriggerHistory findTriggerHistoryForGenericSync() {
        String triggerTableName = TableConstants.getTableName(tablePrefix,
                TableConstants.SYM_TRIGGER);
        TriggerHistory history = engine.getTriggerRouterService().findTriggerHistory(null, null, triggerTableName
                .toUpperCase());
        if (history == null) {
            history = engine.getTriggerRouterService().findTriggerHistory(null, null, triggerTableName);
        }
        return history;
    }

    public void insertSqlEvent(final Node targetNode, String sql, boolean isLoad, long loadId, String createBy) {
        TriggerHistory history = findTriggerHistoryForGenericSync();
        Data data = new Data(history.getSourceTableName(), DataEventType.SQL,
                CsvUtils.escapeCsvData(sql), null, history, Constants.CHANNEL_CONFIG, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(),
                Constants.UNKNOWN_ROUTER_ID, isLoad, loadId, createBy);
    }
    
    public void insertSqlEvent(ISqlTransaction transaction, Node targetNode, String sql,
            boolean isLoad, long loadId, String createBy) {
        TriggerHistory history = findTriggerHistoryForGenericSync();
        insertSqlEvent(transaction, history, Constants.CHANNEL_CONFIG, targetNode, sql, isLoad, loadId, createBy);
    }

    protected void insertSqlEvent(ISqlTransaction transaction, TriggerHistory history, String channelId, Node targetNode, String sql,
            boolean isLoad, long loadId, String createBy) {
        Data data = new Data(history.getSourceTableName(), DataEventType.SQL,
                CsvUtils.escapeCsvData(sql), null, history, isLoad ? Constants.CHANNEL_RELOAD : channelId, null, null);
        insertDataAndDataEventAndOutgoingBatch(transaction, data, targetNode.getNodeId(),
                Constants.UNKNOWN_ROUTER_ID, isLoad, loadId, createBy);
    }

    public int countDataInRange(long firstDataId, long secondDataId) {
        return sqlTemplate.queryForInt(getSql("countDataInRangeSql"), firstDataId, secondDataId);
    }

    public void checkForAndUpdateMissingChannelIds(long firstDataId, long lastDataId) {
        int numberUpdated = sqlTemplate.update(getSql("checkForAndUpdateMissingChannelIdSql"),
                Constants.CHANNEL_DEFAULT, firstDataId, lastDataId);
        if (numberUpdated > 0) {
            log.warn(
                    "There were {} data records found between {} and {} that an invalid channel_id.  Updating them to be on the '{}' channel.",
                    new Object[] { numberUpdated, firstDataId, lastDataId,
                            Constants.CHANNEL_DEFAULT });
        }
    }

    public void insertCreateEvent(final Node targetNode, final TriggerRouter triggerRouter,
            TriggerHistory triggerHistory, String xml, boolean isLoad, long loadId, String createBy) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            insertCreateEvent(transaction, targetNode, triggerRouter, triggerHistory, xml, isLoad, loadId, createBy);
            transaction.commit();
        } finally {
            close(transaction);
        }
    }

    public void insertCreateEvent(ISqlTransaction transaction, Node targetNode,
            TriggerRouter triggerRouter, TriggerHistory triggerHistory, String xml, boolean isLoad, long loadId, String createBy) {
        Trigger trigger = triggerRouter.getTrigger();
        Data data = new Data(
                triggerHistory.getSourceTableName(),
                DataEventType.CREATE,
                CsvUtils.escapeCsvData(xml),
                null,
                triggerHistory,
                parameterService.is(ParameterConstants.INITIAL_LOAD_USE_RELOAD_CHANNEL) && isLoad ? Constants.CHANNEL_RELOAD
                        : trigger.getChannelId(), null, null);
        try {
            insertDataAndDataEventAndOutgoingBatch(transaction, data, targetNode.getNodeId(),
                    triggerRouter.getRouter().getRouterId(), isLoad, loadId, createBy);
        } catch (UniqueKeyException e) {
            if (e.getRootCause() != null && e.getRootCause() instanceof DataTruncation) {
                log.error("Table data definition XML was too large and failed.  The feature to send table creates during the initial load may be limited on your platform.  You may need to set the initial.load.create.first parameter to false.");
            }
            throw e;
        }
    }

    public long insertData(Data data) {
        ISqlTransaction transaction = null;
        long dataId = -1;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            dataId = insertData(transaction, data);
            transaction.commit();
            return dataId;
        } finally {
            close(transaction);
        }
    }

    protected long insertData(ISqlTransaction transaction, final Data data) {
        long id = transaction.insertWithGeneratedKey(
                getSql("insertIntoDataSql"),
                symmetricDialect.getSequenceKeyName(SequenceIdentifier.DATA),
                symmetricDialect.getSequenceName(SequenceIdentifier.DATA),
                new Object[] {
                        data.getTableName(),
                        data.getDataEventType().getCode(),
                        data.getRowData(),
                        data.getPkData(),
                        data.getOldData(),
                        data.getTriggerHistory() != null ? data.getTriggerHistory()
                                .getTriggerHistoryId() : -1, data.getChannelId() }, new int[] {
                        Types.VARCHAR, Types.CHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.NUMERIC, Types.VARCHAR });
        data.setDataId(id);
        return id;
    }

    protected void insertDataEvent(ISqlTransaction transaction, DataEvent dataEvent) {
        this.insertDataEvent(transaction, dataEvent.getDataId(), dataEvent.getBatchId(),
                dataEvent.getRouterId());
    }

    protected void insertDataEvent(ISqlTransaction transaction, long dataId, long batchId,
            String routerId) {
        try {
            transaction
                    .prepareAndExecute(getSql("insertIntoDataEventSql"),
                            new Object[] {
                                    dataId,
                                    batchId,
                                    StringUtils.isBlank(routerId) ? Constants.UNKNOWN_ROUTER_ID
                                            : routerId }, new int[] { Types.NUMERIC, Types.NUMERIC,
                                    Types.VARCHAR });
        } catch (RuntimeException ex) {
            log.error("Could not insert a data event: data_id={} batch_id={} router_id={}",
                    new Object[] { dataId, batchId, routerId });
            log.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    public void insertDataEvents(ISqlTransaction transaction, final List<DataEvent> events) {
        if (events.size() > 0) {
            transaction.prepare(getSql("insertIntoDataEventSql"));
            for (DataEvent dataEvent : events) {
                String routerId = dataEvent.getRouterId();
                transaction.addRow(
                        dataEvent,
                        new Object[] {
                                dataEvent.getDataId(),
                                dataEvent.getBatchId(),
                                StringUtils.isBlank(routerId) ? Constants.UNKNOWN_ROUTER_ID
                                        : routerId }, new int[] { Types.NUMERIC, Types.NUMERIC,
                                Types.VARCHAR });
            }
            transaction.flush();
        }
    }

    public void insertDataAndDataEventAndOutgoingBatch(Data data, String channelId,
            List<Node> nodes, String routerId, boolean isLoad, long loadId, String createBy) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            long dataId = insertData(transaction, data);
            for (Node node : nodes) {
                insertDataEventAndOutgoingBatch(transaction, dataId, channelId, node.getNodeId(),
                        data.getDataEventType(), routerId, isLoad, loadId, createBy);
            }
            transaction.commit();
        } finally {
            close(transaction);
        }
    }

    public void insertDataAndDataEventAndOutgoingBatch(Data data, String nodeId, String routerId,
            boolean isLoad, long loadId, String createBy) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            insertDataAndDataEventAndOutgoingBatch(transaction, data, nodeId, routerId, isLoad, loadId, createBy);
            transaction.commit();
        } finally {
            close(transaction);
        }
    }

    public void insertDataAndDataEventAndOutgoingBatch(ISqlTransaction transaction, Data data,
            String nodeId, String routerId, boolean isLoad, long loadId, String createBy) {
        long dataId = insertData(transaction, data);
        insertDataEventAndOutgoingBatch(transaction, dataId, data.getChannelId(), nodeId,
                data.getDataEventType(), routerId, isLoad, loadId, createBy);
    }

    protected void insertDataEventAndOutgoingBatch(ISqlTransaction transaction, long dataId,
            String channelId, String nodeId, DataEventType eventType, String routerId,
            boolean isLoad, long loadId, String createBy) {
        OutgoingBatch outgoingBatch = new OutgoingBatch(
                nodeId,
                parameterService.is(ParameterConstants.INITIAL_LOAD_USE_RELOAD_CHANNEL) && isLoad ? Constants.CHANNEL_RELOAD
                        : channelId, Status.NE);
        outgoingBatch.setLoadId(loadId);
        outgoingBatch.setCreateBy(createBy);
        outgoingBatch.setLoadFlag(isLoad);
        outgoingBatch.incrementEventCount(eventType);
        engine.getOutgoingBatchService().insertOutgoingBatch(transaction, outgoingBatch);
        insertDataEvent(transaction, new DataEvent(dataId, outgoingBatch.getBatchId(), routerId));
    }

    public String reloadNode(String nodeId, boolean reverseLoad, String createBy) {
        INodeService nodeService = engine.getNodeService();
        Node targetNode = engine.getNodeService().findNode(nodeId);
        if (targetNode == null) {
            return String.format("Unknown node %s", nodeId);
        } else if (reverseLoad && nodeService.setReverseInitialLoadEnabled(nodeId, true, true, createBy)) {
            return String.format("Successfully enabled reverse initial load for node %s", nodeId);
        } else if (nodeService.setInitialLoadEnabled(nodeId, true, true, createBy)) {
            return String.format("Successfully enabled initial load for node %s", nodeId);
        } else {
            return String.format("Could not enable initial load for %s", nodeId);
        }
    }

    private Map<Integer, List<TriggerRouter>> fillTriggerRoutersByHistIdAndSortHist(
            Node sourceNode, Node targetNode, List<TriggerHistory> triggerHistories) {

        List<TriggerRouter> triggerRouters = new ArrayList<TriggerRouter>(
                engine.getTriggerRouterService().getAllTriggerRoutersForReloadForCurrentNode(
                        sourceNode.getNodeGroupId(), targetNode.getNodeGroupId()));

        final Map<Integer, List<TriggerRouter>> triggerRoutersByHistoryId = new HashMap<Integer, List<TriggerRouter>>(
                triggerHistories.size());

        for (TriggerHistory triggerHistory : triggerHistories) {
            List<TriggerRouter> triggerRoutersForTriggerHistory = new ArrayList<TriggerRouter>();
            triggerRoutersByHistoryId.put(triggerHistory.getTriggerHistoryId(),
                    triggerRoutersForTriggerHistory);

            String triggerId = triggerHistory.getTriggerId();
            for (TriggerRouter triggerRouter : triggerRouters) {
                if (triggerRouter.getTrigger().getTriggerId().equals(triggerId)) {
                    triggerRoutersForTriggerHistory.add(triggerRouter);
                }
            }
        }

        final List<Table> sortedTables = getSortedTablesFor(triggerHistories);

        Comparator<TriggerHistory> comparator = new Comparator<TriggerHistory>() {
            public int compare(TriggerHistory o1, TriggerHistory o2) {
                List<TriggerRouter> triggerRoutersForTriggerHist1 = triggerRoutersByHistoryId
                        .get(o1.getTriggerHistoryId());
                int intialLoadOrder1 = 0;
                for (TriggerRouter triggerRouter1 : triggerRoutersForTriggerHist1) {
                    if (triggerRouter1.getInitialLoadOrder() > intialLoadOrder1) {
                        intialLoadOrder1 = triggerRouter1.getInitialLoadOrder();
                    }
                }

                List<TriggerRouter> triggerRoutersForTriggerHist2 = triggerRoutersByHistoryId
                        .get(o2.getTriggerHistoryId());
                int intialLoadOrder2 = 0;
                for (TriggerRouter triggerRouter2 : triggerRoutersForTriggerHist2) {
                    if (triggerRouter2.getInitialLoadOrder() > intialLoadOrder2) {
                        intialLoadOrder2 = triggerRouter2.getInitialLoadOrder();
                    }
                }

                if (intialLoadOrder1 < intialLoadOrder2) {
                    return -1;
                } else if (intialLoadOrder1 > intialLoadOrder2) {
                    return 1;
                }

                Table table1 = platform.getTableFromCache(o1.getSourceCatalogName(),
                        o1.getSourceSchemaName(), o1.getSourceTableName(), false);
                Table table2 = platform.getTableFromCache(o2.getSourceCatalogName(),
                        o2.getSourceSchemaName(), o2.getSourceTableName(), false);

                return new Integer(sortedTables.indexOf(table1)).compareTo(new Integer(sortedTables
                        .indexOf(table2)));
            };
        };

        Collections.sort(triggerHistories, comparator);

        return triggerRoutersByHistoryId;

    }

    protected List<Table> getSortedTablesFor(List<TriggerHistory> histories) {
        List<Table> tables = new ArrayList<Table>(histories.size());
        for (TriggerHistory triggerHistory : histories) {
            Table table = platform.getTableFromCache(triggerHistory.getSourceCatalogName(),
                    triggerHistory.getSourceSchemaName(), triggerHistory.getSourceTableName(),
                    false);
            if (table != null) {
                tables.add(table);
            }
        }
        return Database.sortByForeignKeys(tables);
    }

    private void insertNodeSecurityUpdate(ISqlTransaction transaction, String nodeIdRecord, String targetNodeId, boolean isReload, long loadId, String createBy) {
        Data data = createData(transaction, null, null, tablePrefix + "_node_security",
                " t.node_id = '" + nodeIdRecord + "'");
        if (data != null) {
            insertDataAndDataEventAndOutgoingBatch(transaction, data, targetNodeId,
                    Constants.UNKNOWN_ROUTER_ID, isReload, loadId, createBy);
        }
    }

    public void sendScript(String nodeId, String script, boolean isLoad) {
        Node targetNode = engine.getNodeService().findNode(nodeId);
        TriggerHistory history = findTriggerHistoryForGenericSync();
        Data data = new Data(history.getSourceTableName(), DataEventType.BSH,
                CsvUtils.escapeCsvData(script), null, history, Constants.CHANNEL_CONFIG, null, null);
        insertDataAndDataEventAndOutgoingBatch(data, targetNode.getNodeId(),
                Constants.UNKNOWN_ROUTER_ID, isLoad, -1, null);
    }
    
    public boolean sendSchema(String nodeId, String catalogName, String schemaName,
            String tableName, boolean isLoad) {
        Node sourceNode = engine.getNodeService().findIdentity();
        Node targetNode = engine.getNodeService().findNode(nodeId);
        if (targetNode == null) {
            log.error("Could not send schema to the node {}.  It does not exist", nodeId);
            return false;
        }

        ITriggerRouterService triggerRouterService = engine.getTriggerRouterService();
        List<TriggerHistory> triggerHistories = triggerRouterService.findTriggerHistories(
                catalogName, schemaName, tableName);
        Map<Integer, List<TriggerRouter>> triggerRoutersByHistoryId = fillTriggerRoutersByHistIdAndSortHist(
                sourceNode, targetNode, triggerHistories);
        int eventCount = 0;
        for (TriggerHistory triggerHistory : triggerHistories) {
            List<TriggerRouter> triggerRouters = triggerRoutersByHistoryId.get(triggerHistory
                    .getTriggerHistoryId());
            for (TriggerRouter triggerRouter : triggerRouters) {
                eventCount++;
                String xml = symmetricDialect.getCreateTableXML(triggerHistory, triggerRouter);
                insertCreateEvent(targetNode, triggerRouter, triggerHistory, xml, false, -1, null);
            }
        }

        if (eventCount > 0) {
            return true;
        } else {
            return false;
        }
    }

    public String sendSQL(String nodeId, String catalogName, String schemaName, String tableName,
            String sql, boolean isLoad) {
        Node sourceNode = engine.getNodeService().findIdentity();
        Node targetNode = engine.getNodeService().findNode(nodeId);
        if (targetNode == null) {
            return "Unknown node " + nodeId;
        }

        ITriggerRouterService triggerRouterService = engine.getTriggerRouterService();
        TriggerHistory triggerHistory = triggerRouterService.findTriggerHistory(catalogName,
                schemaName, tableName);

        if (triggerHistory == null) {
            return "Trigger for table " + tableName + " does not exist from node "
                    + sourceNode.getNodeGroupId();
        } else {
            insertSqlEvent(targetNode, triggerHistory, sql, isLoad, -1, null);
            return "Successfully create SQL event for node " + targetNode.getNodeId();
        }
    }

    public String reloadTable(String nodeId, String catalogName, String schemaName, String tableName) {
        return reloadTable(nodeId, catalogName, schemaName, tableName, null);
    }

    public String reloadTable(String nodeId, String catalogName, String schemaName,
            String tableName, String overrideInitialLoadSelect) {
        Node sourceNode = engine.getNodeService().findIdentity();
        Node targetNode = engine.getNodeService().findNode(nodeId);
        if (targetNode == null) {
            return "Unknown node " + nodeId;
        }

        ITriggerRouterService triggerRouterService = engine.getTriggerRouterService();
        List<TriggerHistory> triggerHistories = triggerRouterService.findTriggerHistories(
                catalogName, schemaName, tableName);
        Map<Integer, List<TriggerRouter>> triggerRoutersByHistoryId = fillTriggerRoutersByHistIdAndSortHist(
                sourceNode, targetNode, triggerHistories);
        int eventCount = 0;
        for (TriggerHistory triggerHistory : triggerHistories) {
            List<TriggerRouter> triggerRouters = triggerRoutersByHistoryId.get(triggerHistory
                    .getTriggerHistoryId());
            if (triggerRouters != null && triggerRouters.size() > 0) {
                for (TriggerRouter triggerRouter : triggerRouters) {
                    if (parameterService
                            .is(ParameterConstants.INITIAL_LOAD_CREATE_SCHEMA_BEFORE_RELOAD)) {
                        String xml = symmetricDialect.getCreateTableXML(triggerHistory,
                                triggerRouter);
                        insertCreateEvent(targetNode, triggerRouter, triggerHistory, xml, true, -1, null);
                    } else if (parameterService
                            .is(ParameterConstants.INITIAL_LOAD_DELETE_BEFORE_RELOAD)) {
                        insertPurgeEvent(targetNode, triggerRouter, triggerHistory, true, -1, null);
                    }
                    eventCount++;
                    insertReloadEvent(targetNode, triggerRouter, overrideInitialLoadSelect);
                }
            }
        }

        if (eventCount > 0) {
            return "Successfully created " + (eventCount > 1 ? eventCount + " events" : "event")
                    + " to reload table " + tableName + " for node "

                    + targetNode.getNodeId();
        } else {
            return "Trigger for table " + tableName + " does not exist for source node group of "
                    + sourceNode.getNodeGroupId();
        }

    }

    /**
     * Because we can't add a trigger on the _node table, we are artificially
     * generating heartbeat events.
     * 
     * @param node
     */
    public void insertHeartbeatEvent(Node node, boolean isReload) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            String tableName = TableConstants.getTableName(tablePrefix, TableConstants.SYM_NODE);
            List<NodeGroupLink> links = engine.getConfigurationService().getNodeGroupLinksFor(parameterService
                    .getNodeGroupId());
            for (NodeGroupLink nodeGroupLink : links) {
                if (nodeGroupLink.getDataEventAction() == NodeGroupLinkAction.P) {
                    Set<TriggerRouter> triggerRouters = engine.getTriggerRouterService()
                            .getTriggerRouterForTableForCurrentNode(nodeGroupLink, null, null,
                                    tableName, false);
                    if (triggerRouters != null && triggerRouters.size() > 0) {
                        Data data = createData(transaction, triggerRouters.iterator().next()
                                .getTrigger(), String.format(" t.node_id = '%s'", node.getNodeId()));
                        if (data != null) {
                            insertData(transaction, data);
                        } else {
                            log.warn(
                                    "Not generating data/data events for table {} because a trigger or trigger hist is not created yet.",
                                    tableName);
                        }
                    } else {
                        log.warn(
                                "Not generating data/data events for table {} because a trigger or trigger hist is not created yet.",
                                tableName);
                    }
                }
            }
            transaction.commit();
        } finally {
            close(transaction);
        }

    }

    public Data createData(String catalogName, String schemaName, String tableName) {
        return createData(catalogName, schemaName, tableName, null);
    }

    public Data createData(String catalogName, String schemaName, String tableName,
            String whereClause) {
        ISqlTransaction transaction = null;
        try {
            transaction = sqlTemplate.startSqlTransaction();
            Data data = createData(transaction, catalogName, schemaName, tableName, whereClause);
            transaction.commit();
            return data;
        } finally {
            close(transaction);
        }
    }

    public Data createData(ISqlTransaction transaction, String catalogName, String schemaName,
            String tableName, String whereClause) {
        Data data = null;
        Set<TriggerRouter> triggerRouters = engine.getTriggerRouterService()
                .getTriggerRouterForTableForCurrentNode(catalogName, schemaName, tableName, false);
        if (triggerRouters != null && triggerRouters.size() > 0) {
            data = createData(transaction, triggerRouters.iterator().next().getTrigger(),
                    whereClause);
        }
        return data;
    }

    protected Data createData(ISqlTransaction transaction, Trigger trigger, String whereClause) {
        Data data = null;
        if (trigger != null) {
            TriggerHistory triggerHistory = engine.getTriggerRouterService().getNewestTriggerHistoryForTrigger(
                    trigger.getTriggerId());
            if (triggerHistory == null) {
                triggerHistory = engine.getTriggerRouterService().findTriggerHistory(trigger.getSourceCatalogName(),
                        trigger.getSourceSchemaName(), trigger
                        .getSourceTableName());
                if (triggerHistory == null) {
                    triggerHistory = engine.getTriggerRouterService().findTriggerHistory(trigger.getSourceCatalogName(),
                            trigger.getSourceSchemaName(), trigger
                            .getSourceTableName().toUpperCase());
                }
            }
            if (triggerHistory != null) {
                String rowData = null;
                String pkData = null;
                if (whereClause != null) {
                    rowData = (String) transaction.queryForObject(symmetricDialect
                            .createCsvDataSql(trigger, triggerHistory,
                                    engine.getConfigurationService().getChannel(trigger.getChannelId()),
                                    whereClause), String.class);
                    if (rowData != null) {
                        rowData = rowData.trim();
                    }
                    pkData = (String) transaction.queryForObject(symmetricDialect
                            .createCsvPrimaryKeySql(trigger, triggerHistory,
                                    engine.getConfigurationService().getChannel(trigger.getChannelId()),
                                    whereClause), String.class);
                    if (pkData != null) {
                        pkData = pkData.trim();
                    }
                }
                data = new Data(trigger.getSourceTableName(), DataEventType.UPDATE, rowData,
                        pkData, triggerHistory, trigger.getChannelId(), null, null);
            }
        }
        return data;
    }

    public List<DataGap> findDataGapsByStatus(DataGap.Status status) {
        return sqlTemplate.query(getSql("findDataGapsByStatusSql"), new ISqlRowMapper<DataGap>() {
            public DataGap mapRow(Row rs) {
                return new DataGap(rs.getLong("start_id"), rs.getLong("end_id"), rs
                        .getDateTime("create_time"));
            }
        }, status.name());
    }

    public List<DataGap> findDataGaps() {
        final long maxDataToSelect = parameterService
                .getInt(ParameterConstants.ROUTING_LARGEST_GAP_SIZE);
        List<DataGap> gaps = findDataGapsByStatus(DataGap.Status.GP);
        boolean lastGapExists = false;
        for (DataGap dataGap : gaps) {
            lastGapExists |= dataGap.gapSize() >= maxDataToSelect - 1;
        }

        if (!lastGapExists) {
            long maxDataId = findMaxDataEventDataId();
            if (maxDataId > 0) {
                maxDataId++;
            }
            insertDataGap(new DataGap(maxDataId, maxDataId + maxDataToSelect));
            gaps = findDataGaps();
        }
        return gaps;

    }

    public long findMaxDataEventDataId() {
        return sqlTemplate.queryForLong(getSql("selectMaxDataEventDataIdSql"));
    }

    public void insertDataGap(DataGap gap) {
        try {
            sqlTemplate.update(getSql("insertDataGapSql"), new Object[] { DataGap.Status.GP.name(),
                    AppUtils.getHostName(), gap.getStartId(), gap.getEndId() }, new int[] {
                    Types.VARCHAR, Types.VARCHAR, Types.NUMERIC, Types.NUMERIC });
        } catch (UniqueKeyException ex) {
            log.warn("A gap already existed for {} to {}.  Updating instead.", gap.getStartId(),
                    gap.getEndId());
            updateDataGap(gap, DataGap.Status.GP);
        }
    }

    public void updateDataGap(DataGap gap, DataGap.Status status) {
        sqlTemplate.update(
                getSql("updateDataGapSql"),
                new Object[] { status.name(), AppUtils.getHostName(), gap.getStartId(),
                        gap.getEndId() }, new int[] { Types.VARCHAR, Types.VARCHAR, Types.NUMERIC,
                        Types.NUMERIC });
    }

    public void deleteDataGap(DataGap gap) {
        sqlTemplate.update(getSql("deleteDataGapSql"),
                new Object[] { gap.getStartId(), gap.getEndId() }, new int[] { Types.NUMERIC,
                        Types.NUMERIC });

    }

    public Date findCreateTimeOfEvent(long dataId) {
        return sqlTemplate.queryForObject(getSql("findDataEventCreateTimeSql"), Date.class, dataId);
    }

    public Date findCreateTimeOfData(long dataId) {
        return sqlTemplate.queryForObject(getSql("findDataCreateTimeSql"), Date.class, dataId);
    }

//    public Map<String, String> getRowDataAsMap(Data data) {
//        Map<String, String> map = new HashMap<String, String>();
//        String[] columnNames = CsvUtils.tokenizeCsvData(data.getTriggerHistory().getColumnNames());
//        String[] columnData = CsvUtils.tokenizeCsvData(data.getRowData());
//        for (int i = 0; i < columnNames.length; i++) {
//            map.put(columnNames[i].toLowerCase(), columnData[i]);
//        }
//        return map;
//    }

    /**
     * Get a list of {@link IHeartbeatListener}s that are ready for a heartbeat
     * according to
     * {@link IHeartbeatListener#getTimeBetweenHeartbeatsInSeconds()}
     * 
     * @param force
     *            if true, then return the entire list of
     *            {@link IHeartbeatListener}s
     */
    protected List<IHeartbeatListener> getHeartbeatListeners(boolean force) {
        if (force) {
            return this.heartbeatListeners;
        } else {
            List<IHeartbeatListener> listeners = new ArrayList<IHeartbeatListener>();
            if (listeners != null) {
                long ts = System.currentTimeMillis();
                for (IHeartbeatListener iHeartbeatListener : this.heartbeatListeners) {
                    Long lastHeartbeatTimestamp = lastHeartbeatTimestamps.get(iHeartbeatListener);
                    if (lastHeartbeatTimestamp == null
                            || lastHeartbeatTimestamp <= ts
                                    - (iHeartbeatListener.getTimeBetweenHeartbeatsInSeconds() * 1000)) {
                        listeners.add(iHeartbeatListener);
                    }
                }
            }
            return listeners;
        }
    }

    protected void updateLastHeartbeatTime(List<IHeartbeatListener> listeners) {
        if (listeners != null) {
            Long ts = System.currentTimeMillis();
            for (IHeartbeatListener iHeartbeatListener : listeners) {
                lastHeartbeatTimestamps.put(iHeartbeatListener, ts);
            }
        }
    }

    /**
     * @see IDataService#heartbeat()
     */
    public void heartbeat(boolean force) {
        List<IHeartbeatListener> listeners = getHeartbeatListeners(force);
        if (listeners.size() > 0) {
            Node me = engine.getNodeService().findIdentity();
            if (me != null) {
                for (IHeartbeatListener l : listeners) {
                    l.heartbeat(me);
                }
                updateLastHeartbeatTime(listeners);
            } else {
                log.debug("Did not run the heartbeat process because the node has not been configured");
            }
        }
    }

    public void setReloadListeners(List<IReloadListener> listeners) {
        this.reloadListeners = listeners;
    }

    public void addReloadListener(IReloadListener listener) {
        if (reloadListeners == null) {
            reloadListeners = new ArrayList<IReloadListener>();
        }
        reloadListeners.add(listener);
    }

    public boolean removeReloadListener(IReloadListener listener) {
        if (reloadListeners != null) {
            return reloadListeners.remove(listener);
        } else {
            return false;
        }
    }

    public void setHeartbeatListeners(List<IHeartbeatListener> listeners) {
        this.heartbeatListeners = listeners;
    }

    public void addHeartbeatListener(IHeartbeatListener listener) {
        if (heartbeatListeners == null) {
            heartbeatListeners = new ArrayList<IHeartbeatListener>();
        }
        heartbeatListeners.add(listener);
    }

    public boolean removeHeartbeatListener(IHeartbeatListener listener) {
        if (heartbeatListeners != null) {
            return heartbeatListeners.remove(listener);
        } else {
            return false;
        }
    }

    public List<Number> listDataIds(long batchId, String nodeId) {
        return sqlTemplate.query(getSql("selectEventDataIdsSql", " order by d.data_id asc"),
                new NumberMapper(), batchId, nodeId);
    }

    public List<Data> listData(long batchId, String nodeId, long startDataId, String channelId,
            final int maxRowsToRetrieve) {
        return sqlTemplate.query(getDataSelectSql(batchId, startDataId, channelId),
                maxRowsToRetrieve, this.dataMapper, batchId, nodeId, startDataId);
    }

    public Data mapData(Row row) {
        return dataMapper.mapRow(row);
    }

    public ISqlReadCursor<Data> selectDataFor(Batch batch) {
        return sqlTemplate
                .queryForCursor(getDataSelectSql(batch.getBatchId(), -1l, batch.getChannelId()),
                        dataMapper, new Object[] { batch.getBatchId(), batch.getTargetNodeId() },
                        new int[] { Types.NUMERIC });
    }

    protected String getDataSelectSql(long batchId, long startDataId, String channelId) {
        String startAtDataIdSql = startDataId >= 0l ? " and d.data_id >= ? " : "";
        return symmetricDialect.massageDataExtractionSql(
                getSql("selectEventDataToExtractSql", startAtDataIdSql, " order by d.data_id asc"),
                engine.getConfigurationService().getNodeChannel(channelId, false).getChannel());
    }

    public long findMaxDataId() {
        return sqlTemplate.queryForLong(getSql("selectMaxDataIdSql"));
    }

    public class DataMapper implements ISqlRowMapper<Data> {
        public Data mapRow(Row row) {
            Data data = new Data();
            data.putCsvData(CsvData.ROW_DATA, row.getString("ROW_DATA", false));
            data.putCsvData(CsvData.PK_DATA, row.getString("PK_DATA", false));
            data.putCsvData(CsvData.OLD_DATA, row.getString("OLD_DATA", false));
            data.putAttribute(CsvData.ATTRIBUTE_CHANNEL_ID, row.getString("CHANNEL_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_TX_ID, row.getString("TRANSACTION_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_TABLE_NAME, row.getString("TABLE_NAME"));
            data.setDataEventType(DataEventType.getEventType(row.getString("EVENT_TYPE")));
            data.putAttribute(CsvData.ATTRIBUTE_SOURCE_NODE_ID, row.getString("SOURCE_NODE_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_EXTERNAL_DATA, row.getString("EXTERNAL_DATA"));
            data.putAttribute(CsvData.ATTRIBUTE_DATA_ID, row.getLong("DATA_ID"));
            data.putAttribute(CsvData.ATTRIBUTE_CREATE_TIME, row.getDateTime("CREATE_TIME"));
            data.putAttribute(CsvData.ATTRIBUTE_ROUTER_ID, row.getString("ROUTER_ID", false));
            int triggerHistId = row.getInt("TRIGGER_HIST_ID");
            data.putAttribute(CsvData.ATTRIBUTE_TABLE_ID, triggerHistId);
            data.setTriggerHistory(engine.getTriggerRouterService().getTriggerHistory(triggerHistId));
            if (data.getTriggerHistory() == null) {
                data.setTriggerHistory(new TriggerHistory(triggerHistId));
            }
            return data;
        }
    }

}
