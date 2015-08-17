package org.jumpmind.util;
/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyedCache<K, T> implements Serializable {

    private static final long serialVersionUID = 1L;

    long timeoutTimeInMs;

    long lastRefreshTimeMs = 0;

    protected LinkedHashMap<K, T> keyedCache = new LinkedHashMap<K, T>();

    protected ICacheRefresher<K, T> refresher;

    public KeyedCache(long timeoutTimeInMs, ICacheRefresher<K, T> refresher) {
        this.timeoutTimeInMs = timeoutTimeInMs;
        this.refresher = refresher;
    }
    
    public boolean containsKey(String key) {
        refreshCacheIfNeeded(false);
        return keyedCache.containsKey(key);
    }

    public T find(K key, boolean refreshCache) {
        refreshCacheIfNeeded(refreshCache);
        return keyedCache.get(key);
    }

    public List<T> getAll(boolean refreshCache) {
        refreshCacheIfNeeded(refreshCache);
        return new ArrayList<T>(keyedCache.values());
    }

    public void clear() {
        lastRefreshTimeMs = 0;
    }

    public void refreshCacheIfNeeded(boolean refreshCache) {
        Map<K, T> copy = keyedCache;
        if (copy == null || refreshCache || (System.currentTimeMillis() - lastRefreshTimeMs) > timeoutTimeInMs) {
            synchronized (this) {
                if (copy == null || refreshCache || (System.currentTimeMillis() - lastRefreshTimeMs) > timeoutTimeInMs) {
                    refreshCache();
                }
            }
        }
    }
    
    protected void refreshCache() {
        keyedCache = refresher.refresh();
        lastRefreshTimeMs = System.currentTimeMillis();
    }

    public interface ICacheRefresher<K, T> {
        public LinkedHashMap<K, T> refresh();
    }

}
