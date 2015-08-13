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

package org.jumpmind.symmetric4.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the trigger via which a table will be synchronized.
 */
public class TriggerRouter extends AbstractObject {

    private static final long serialVersionUID = 1L;

    static final Logger logger = LoggerFactory.getLogger(TriggerRouter.class);

    private boolean enabled = true;

    /**
     * This is the order in which the definitions will be processed.
     */
    private int initialLoadOrder = 50;

    private String initialLoadSelect;

    private String initialLoadDeleteStmt;

    private int initialLoadBatchCount = 1;

    private String triggerId;

    private String routerId;

    private boolean pingBackEnabled = false;

    public TriggerRouter() {
    }

    public TriggerRouter(String triggerId, String routerId) {
        this.triggerId = triggerId;
        this.routerId = routerId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getInitialLoadOrder() {
        return initialLoadOrder;
    }

    public void setInitialLoadOrder(int order) {
        this.initialLoadOrder = order;
    }

    public void setInitialLoadSelect(String initialLoadSelect) {
        this.initialLoadSelect = initialLoadSelect;
    }

    public String getInitialLoadSelect() {
        return initialLoadSelect;
    }

    public String getInitialLoadDeleteStmt() {
        return initialLoadDeleteStmt;
    }

    public void setInitialLoadDeleteStmt(String initialLoadDeleteStmt) {
        this.initialLoadDeleteStmt = initialLoadDeleteStmt;
    }

    public void setInitialLoadBatchCount(int initialLoadBatchCount) {
        this.initialLoadBatchCount = initialLoadBatchCount;
    }

    public int getInitialLoadBatchCount() {
        return initialLoadBatchCount;
    }

    public void setPingBackEnabled(boolean pingBackEnabled) {
        this.pingBackEnabled = pingBackEnabled;
    }

    public boolean isPingBackEnabled() {
        return pingBackEnabled;
    }

    public boolean isSame(TriggerRouter triggerRouter) {
        return (this.triggerId == null && triggerRouter.triggerId == null)
                || (this.triggerId != null && triggerRouter.triggerId != null && this.triggerId.equals(triggerRouter.triggerId))
                        && (this.routerId == null && triggerRouter.routerId == null)
                || (this.routerId != null && triggerRouter.routerId != null && this.routerId.equals(triggerRouter.routerId));
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public String getTriggerId() {
        return triggerId;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", triggerId, routerId);
    }

}