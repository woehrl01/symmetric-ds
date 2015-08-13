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

import org.jumpmind.symmetric.common.ParameterConstants;

public class Parameter extends AbstractObject {

    private static final long serialVersionUID = 1L;

    private String paramKey;
    private String paramValue;
    private String externalId = ParameterConstants.ALL;
    private String nodeGroupId = ParameterConstants.ALL;

    public Parameter() {
    }
    
    public Parameter(String key) {
        this.paramKey = key;
    }

    public Parameter(String key, String value, String externalId, String nodeGroupId) {
        this.paramKey = key;
        this.paramValue = value;
        this.externalId = externalId;
        this.nodeGroupId = nodeGroupId;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String key) {
        this.paramKey = key;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String value) {
        this.paramValue = value;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getNodeGroupId() {
        return nodeGroupId;
    }

    public void setNodeGroupId(String nodeGroupId) {
        this.nodeGroupId = nodeGroupId;
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s:%s", externalId, nodeGroupId, paramKey);
    }

}
