/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.policyengine;

import org.apache.ranger.plugin.store.SecurityZoneStore;
import org.apache.ranger.plugin.store.ServiceStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RangerPolicyEngineCacheForEngineOptions {

    private static volatile RangerPolicyEngineCacheForEngineOptions sInstance = null;

    private final Map<RangerPolicyEngineOptions, RangerPolicyEngineCache> policyEngineCacheForEngineOptions = Collections.synchronizedMap(new HashMap<RangerPolicyEngineOptions, RangerPolicyEngineCache>());

    public static RangerPolicyEngineCacheForEngineOptions getInstance() {
        RangerPolicyEngineCacheForEngineOptions ret = sInstance;
        if (ret == null) {
            synchronized (RangerPolicyEngineCacheForEngineOptions.class) {
                ret = sInstance;
                if (ret == null) {
                    sInstance = new RangerPolicyEngineCacheForEngineOptions();
                    ret = sInstance;
                }
            }
        }
        return ret;
    }

    public final RangerPolicyEngine getPolicyEngine(String serviceName, ServiceStore svcStore, RangerPolicyEngineOptions options) {
        return getPolicyEngine(serviceName, svcStore, null, options);
    }

    public final RangerPolicyEngine getPolicyEngine(String serviceName, ServiceStore svcStore, SecurityZoneStore zoneStore, RangerPolicyEngineOptions options) {

        RangerPolicyEngineCache policyEngineCache;

        synchronized (this) {
            policyEngineCache = policyEngineCacheForEngineOptions.get(options);
            if (policyEngineCache == null) {
                policyEngineCache = new RangerPolicyEngineCache();
                policyEngineCacheForEngineOptions.put(options, policyEngineCache);
            }
        }
        return policyEngineCache.getPolicyEngine(serviceName, svcStore, zoneStore, options);
    }
}

