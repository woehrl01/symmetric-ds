package org.jumpmind.symmetric4.service;

import java.nio.channels.Channel;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.ITypedPropertiesFactory;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric4.model.Extension;
import org.jumpmind.symmetric4.model.NodeGroup;
import org.jumpmind.symmetric4.model.NodeGroupLink;
import org.jumpmind.symmetric4.model.Parameter;
import org.jumpmind.symmetric4.model.Router;
import org.jumpmind.symmetric4.model.Trigger;
import org.jumpmind.symmetric4.model.TriggerRouter;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.FormatUtils;
import org.jumpmind.util.KeyedCache.ICacheRefresher;

public class ConfigurationService extends AbstractService {

    protected ITypedPropertiesFactory propertiesFactory;

    protected TypedProperties parameters;

    public ConfigurationService(ITypedPropertiesFactory propertiesFactory, String tablePrefix, long cacheTimeout,
            IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
        this.propertiesFactory = propertiesFactory;
    }
    
    public TypedProperties getParameters() {
        return parameters;
    }

    @Override
    protected Class<?>[] getCachedTypes() {
        return new Class<?>[] { Trigger.class, Router.class, TriggerRouter.class, Parameter.class, NodeGroup.class, NodeGroupLink.class,
                Channel.class };
    }

    @Override
    protected ICacheRefresher<Class<?>, TreeMap<String, Object>> createCacheRefresher() {
        return new DefaultCacheRefresher() {
            @Override
            public LinkedHashMap<Class<?>, TreeMap<String, Object>> refresh() {
                LinkedHashMap<Class<?>, TreeMap<String, Object>> cache = super.refresh();
                TypedProperties newParameters = new TypedProperties();
                TypedProperties properties = propertiesFactory.reload();
                String engineName = properties.get(ParameterConstants.ENGINE_NAME);
                String externalId = properties.get(ParameterConstants.EXTERNAL_ID);
                String nodeGroupId = properties.get(ParameterConstants.NODE_GROUP_ID);
                for (Object key : properties.keySet()) {
                    newParameters.put(key, substituteVariables((String) key, properties.getProperty((String) key), engineName));
                }
                Collection<Object> parameters = cache.get(Parameter.class).values();
                addParameters(parameters, newParameters, ParameterConstants.ALL, ParameterConstants.ALL, engineName);
                addParameters(parameters, newParameters, ParameterConstants.ALL, nodeGroupId, engineName);
                addParameters(parameters, newParameters, externalId, ParameterConstants.ALL, engineName);
                addParameters(parameters, newParameters, externalId, nodeGroupId, engineName);
                return cache;
            }

            protected void addParameters(Collection<Object> parameters, TypedProperties newParameters, String externalId, String nodeGroupId,
                    String engineName) {
                for (Object object : parameters) {
                    Parameter parameter = (Parameter) object;
                    if (parameter.getExternalId().equals(externalId) || parameter.getNodeGroupId().equals(nodeGroupId)) {
                        newParameters.put(parameter.getParamKey(),
                                substituteVariables((String) parameter.getParamKey(), parameter.getParamValue(), engineName));
                    }
                }
            }

            protected String substituteVariables(String paramKey, String value, String engineName) {
                if (!StringUtils.isBlank(value)) {
                    if (value.contains("hostName")) {
                        value = FormatUtils.replace("hostName", AppUtils.getHostName(), value);
                    }
                    if (value.contains("portNumber")) {
                        value = FormatUtils.replace("portNumber", AppUtils.getPortNumber(), value);
                    }
                    if (value.contains("ipAddress")) {
                        value = FormatUtils.replace("ipAddress", AppUtils.getIpAddress(), value);
                    }
                    if (value.contains("engineName")) {
                        value = FormatUtils.replace("engineName", engineName, value);
                    }
                }
                return value;
            }

        };

    }

}
