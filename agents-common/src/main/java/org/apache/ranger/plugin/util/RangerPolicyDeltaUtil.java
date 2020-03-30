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

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RangerPolicyDeltaUtil {

    private static final Log LOG = LogFactory.getLog(RangerPolicyDeltaUtil.class);

    private static final Log PERF_POLICY_DELTA_LOG = RangerPerfTracer.getPerfLogger("policy.delta");

    public static List<RangerPolicy> applyDeltas(List<RangerPolicy> policies, List<RangerPolicyDelta> deltas, String serviceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> applyDeltas(serviceType=" + serviceType + ")");
        }

        List<RangerPolicy> ret;

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_DELTA_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_DELTA_LOG, "RangerPolicyDelta.applyDeltas()");
        }

        if (CollectionUtils.isNotEmpty(deltas)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("applyDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", serviceType=" + serviceType +")");
            }
            ret = new ArrayList<>(policies);

            for (RangerPolicyDelta delta : deltas) {
                int changeType = delta.getChangeType();
                if (!serviceType.equals(delta.getServiceType())) {
                    if (!delta.getServiceType().equals(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
                        LOG.error("Found unexpected serviceType in policyDelta:[" + delta + "]. Was expecting serviceType:[" + serviceType + "]. Should NOT have come here!! Ignoring delta and continuing");
                    }
                    continue;
                }
                if (changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE || changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE || changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                    if (changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE) {
                        if (delta.getPolicy() != null) {
                            ret.add(delta.getPolicy());
                        }
                    } else {
                        // Either UPDATE or DELETE
                        Long policyId       = delta.getPolicyId();

                        Iterator<RangerPolicy> iter = ret.iterator();
                        while (iter.hasNext()) {
                            if (policyId.equals(iter.next().getId())) {
                                iter.remove();
                                break;
                            }
                        }
                        if (changeType == RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE) {
                            if (delta.getPolicy() != null) {
                                ret.add(delta.getPolicy());
                            }
                        }
                    }
                } else {
                    LOG.warn("Found unexpected changeType in policyDelta:[" + delta +"]. Ignoring delta");
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("applyDeltas(deltas=null, serviceType=" + serviceType +")");
            }
            ret = policies;
        }

        if (CollectionUtils.isNotEmpty(deltas) && CollectionUtils.isNotEmpty(ret)) {
            ret.sort(RangerPolicy.POLICY_ID_COMPARATOR);
        }

        RangerPerfTracer.log(perf);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== applyDeltas(serviceType=" + serviceType + "): " + ret);
        }
        return ret;
    }

    public static boolean isValidDeltas(List<RangerPolicyDelta> deltas, String componentServiceType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isValidDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", componentServiceType=" + componentServiceType +")");
        }
        boolean isValid = true;

        for (RangerPolicyDelta delta : deltas) {
            final Integer changeType = delta.getChangeType();
            final Long    policyId   = delta.getPolicyId();

            if (changeType == null) {
                isValid = false;
                break;
            }

            if (changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE
                    && changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE
                    && changeType != RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE) {
                isValid = false;
            } else if (policyId == null) {
                isValid = false;
            } else {
                final String  serviceType = delta.getServiceType();
                final Integer policyType  = delta.getPolicyType();

                if (serviceType == null || (!serviceType.equals(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME) &&
                        !serviceType.equals(componentServiceType))) {
                    isValid = false;
                } else if (policyType == null || (policyType != RangerPolicy.POLICY_TYPE_ACCESS
                        && policyType != RangerPolicy.POLICY_TYPE_DATAMASK
                        && policyType != RangerPolicy.POLICY_TYPE_ROWFILTER)) {
                    isValid = false;
                }
            }

            if (!isValid) {
                break;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isValidDeltas(deltas=" + Arrays.toString(deltas.toArray()) + ", componentServiceType=" + componentServiceType +"): " + isValid);
        }
        return isValid;
    }
}
