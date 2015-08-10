package org.jumpmind.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkThreadFactory {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    Map<String, ExecutorService> executors = new HashMap<String, ExecutorService>();
    Map<String, List<Runnable>> currentRunnables = new HashMap<String, List<Runnable>>();
    Map<String, List<Object>> currentKeys = new HashMap<String, List<Object>>();

    public void createWorkThreadPool(final String poolName, int maxThreadCount) {
        ExecutorService executor = new ThreadPoolExecutor(maxThreadCount, maxThreadCount, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(poolName));
        executors.put(poolName, executor);
        currentRunnables.put(poolName, new ArrayList<Runnable>());
        currentKeys.put(poolName, new ArrayList<Object>());
    }

    /**
     * @throws RejectedExecutionException
     *             if the task cannot be scheduled for execution
     */
    public boolean doWork(String poolName, Runnable runnable) {
        return doWork(poolName, runnable, runnable);
    }

    /**
     * @throws RejectedExecutionException
     *             if the task cannot be scheduled for execution
     */
    public boolean doWork(final String poolName, final Object workKey, final Runnable runnable) {
        ExecutorService executor = executors.get(poolName);
        if (executor == null) {
            throw new IllegalStateException(String.format("An executor pool named %d has not be created", poolName));
        }
        executor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    log.error("", ex);
                } finally {
                    currentRunnables.get(poolName).remove(runnable);
                    currentKeys.get(poolName).remove(workKey);
                }
            }
        });
        currentRunnables.get(poolName).add(runnable);
        currentKeys.get(poolName).add(workKey);
        return true;

    }

    public void shutdownAll() {
        Collection<ExecutorService> list = executors.values();
        for (ExecutorService executor : list) {
            executor.shutdown();
        }
    }
}
