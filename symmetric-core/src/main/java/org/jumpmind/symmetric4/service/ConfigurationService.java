package org.jumpmind.symmetric4.service;

import java.nio.channels.Channel;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.ITypedPropertiesFactory;
import org.jumpmind.symmetric4.model.NodeGroup;
import org.jumpmind.symmetric4.model.NodeGroupLink;
import org.jumpmind.symmetric4.model.Extension;
import org.jumpmind.symmetric4.model.Parameter;
import org.jumpmind.symmetric4.model.Router;
import org.jumpmind.symmetric4.model.Trigger;
import org.jumpmind.symmetric4.model.TriggerRouter;

public class ConfigurationService extends AbstractService {

    protected ITypedPropertiesFactory propertiesFactory;

    public ConfigurationService(ITypedPropertiesFactory propertiesFactory, String tablePrefix, long cacheTimeout,
            IDatabasePlatform platform) {
        super(tablePrefix, cacheTimeout, platform);
        this.propertiesFactory = propertiesFactory;
    }

    @Override
    protected Class<?>[] getCachedTypes() {
        return new Class<?>[] { Trigger.class, Router.class, TriggerRouter.class, Parameter.class, NodeGroup.class, NodeGroupLink.class,
                Channel.class, Extension.class };
    }
    
    public TypedProperties getParameters() {
        return null;
    }

}
