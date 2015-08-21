package org.jumpmind.symmetric4;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.security.ISecurityService;
import org.jumpmind.security.SecurityServiceFactory;
import org.jumpmind.security.SecurityServiceFactory.SecurityServiceType;
import org.jumpmind.symmetric.ITypedPropertiesFactory;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.db.ISymmetricDialect;
import org.jumpmind.symmetric4.service.ConfigurationService;
import org.jumpmind.symmetric4.service.ExtensionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class AbstractSymmetricEngine implements ISymmetricEngine, IApplicationContext {

    private static Map<String, ISymmetricEngine> registeredEnginesByUrl = new HashMap<String, ISymmetricEngine>();
    private static Map<String, ISymmetricEngine> registeredEnginesByName = new HashMap<String, ISymmetricEngine>();

    protected static final Logger log = LoggerFactory.getLogger(AbstractSymmetricEngine.class);

    private boolean started = false;

    private boolean starting = false;

    private boolean setup = false;

    private Date lastRestartTime;

    protected String deploymentType;

    protected ITypedPropertiesFactory propertiesFactory;

    protected IDatabasePlatform platform;

    protected ISecurityService securityService;

    protected ISymmetricDialect symmetricDialect;

    protected ConfigurationService configurationService;

    protected ExtensionService extensionService;


    protected AbstractSymmetricEngine(boolean registerEngine) {
        this.init(registerEngine);
    }

    protected void init(boolean registerEngine) {
        if (propertiesFactory == null) {
            this.propertiesFactory = createTypedPropertiesFactory();
        }

        TypedProperties properties = this.propertiesFactory.reload();

        if (securityService == null) {
            this.securityService = SecurityServiceFactory.create(getSecurityServiceType(), properties);
        }

        MDC.put("engineName", properties.get(ParameterConstants.ENGINE_NAME));

        String tablePrefix = properties.get(ParameterConstants.TABLE_PREFIX);
        long cacheTimeout = properties.getLong(ParameterConstants.CACHE_TIMEOUT_CHANNEL_IN_MS);

        this.platform = createDatabasePlatform(properties);

        this.configurationService = new ConfigurationService(propertiesFactory, tablePrefix, cacheTimeout, platform);

        this.extensionService = new ExtensionService(tablePrefix, cacheTimeout, platform);

        if (registerEngine) {
            registerEngine();
        }
    }
    
    @Override
    public <T> T getService(Class<T> clazz) {
        return null;
    }

    public void start() {
        lastRestartTime = new Date();
    }

    public void stop() {

    }

    abstract protected SecurityServiceType getSecurityServiceType();

    abstract protected ITypedPropertiesFactory createTypedPropertiesFactory();

    abstract protected IDatabasePlatform createDatabasePlatform(TypedProperties properties);

    abstract protected ISymmetricDialect createSymmetricDialect();

    abstract protected ExtensionService createExtensionService();

    /**
     * Register this instance of the engine so it can be found by other
     * processes in the JVM.
     * 
     * @see #findEngineByUrl(String)
     */
    private void registerEngine() {
        // String url = getSyncUrl();
        // ISymmetricEngine alreadyRegister = registeredEnginesByUrl.get(url);
        // if (alreadyRegister == null || alreadyRegister.equals(this)) {
        // if (url != null) {
        // registeredEnginesByUrl.put(url, this);
        // }
        // } else {
        // log.warn("Could not register engine. There was already an engine
        // registered under the url: {}", getSyncUrl());
        // }
        //
        // alreadyRegister = registeredEnginesByName.get(getEngineName());
        // if (alreadyRegister == null || alreadyRegister.equals(this)) {
        // registeredEnginesByName.put(getEngineName(), this);
        // } else {
        // throw new EngineAlreadyRegisteredException(
        // "Could not register engine. There was already an engine registered
        // under the name: " + getEngineName());
        // }

    }

    public Date getLastRestartTime() {
        return lastRestartTime;
    }

    public ISqlTemplate getSqlTemplate() {
        return platform.getSqlTemplate();
    }

    @SuppressWarnings("unchecked")
    public <T> T getDataSource() {
        return (T) platform.getDataSource();
    }

    public IDatabasePlatform getDatabasePlatform() {
        return platform;
    }

    /**
     * Locate a {@link StandaloneSymmetricEngine} in the same JVM
     */
    public static ISymmetricEngine findEngineByUrl(String url) {
        if (registeredEnginesByUrl != null && url != null) {
            return registeredEnginesByUrl.get(url);
        } else {
            return null;
        }
    }

    /**
     * Locate a {@link StandaloneSymmetricEngine} in the same JVM
     */
    public static ISymmetricEngine findEngineByName(String name) {
        if (registeredEnginesByName != null && name != null) {
            return registeredEnginesByName.get(name);
        } else {
            return null;
        }
    }

}
