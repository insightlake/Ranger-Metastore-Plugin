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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerConfiguration;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerDataMaskTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ServiceDefUtil {

    public static boolean getOption_enableDenyAndExceptionsInPolicies(RangerServiceDef serviceDef) {
        boolean ret = false;

        if(serviceDef != null) {
            boolean enableDenyAndExceptionsInPoliciesHiddenOption = RangerConfiguration.getInstance().getBoolean("ranger.servicedef.enableDenyAndExceptionsInPolicies", true);
            boolean defaultValue = enableDenyAndExceptionsInPoliciesHiddenOption || StringUtils.equalsIgnoreCase(serviceDef.getName(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME);

            ret = ServiceDefUtil.getBooleanValue(serviceDef.getOptions(), RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, defaultValue);
        }

        return ret;
    }

    public static RangerDataMaskTypeDef getDataMaskType(RangerServiceDef serviceDef, String typeName) {
        RangerDataMaskTypeDef ret = null;

        if(serviceDef != null && serviceDef.getDataMaskDef() != null) {
            List<RangerDataMaskTypeDef> maskTypes = serviceDef.getDataMaskDef().getMaskTypes();

            if(CollectionUtils.isNotEmpty(maskTypes)) {
                for(RangerDataMaskTypeDef maskType : maskTypes) {
                    if(StringUtils.equals(maskType.getName(), typeName)) {
                        ret = maskType;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    public static RangerServiceDef normalize(RangerServiceDef serviceDef) {
        normalizeDataMaskDef(serviceDef);
        normalizeRowFilterDef(serviceDef);

        return serviceDef;
    }

    public static RangerResourceDef getResourceDef(RangerServiceDef serviceDef, String resource) {
        RangerResourceDef ret = null;

        if(serviceDef != null && resource != null && CollectionUtils.isNotEmpty(serviceDef.getResources())) {
            for(RangerResourceDef resourceDef : serviceDef.getResources()) {
                if(StringUtils.equalsIgnoreCase(resourceDef.getName(), resource)) {
                    ret = resourceDef;
                    break;
                }
            }
        }

        return ret;
    }

    public static Integer getLeafResourceLevel(RangerServiceDef serviceDef, Map<String, RangerPolicy.RangerPolicyResource> policyResource) {
        Integer ret = null;

        RangerResourceDef resourceDef = getLeafResourceDef(serviceDef, policyResource);

        if (resourceDef != null) {
            ret = resourceDef.getLevel();
        }

        return ret;
    }

    public static RangerResourceDef getLeafResourceDef(RangerServiceDef serviceDef, Map<String, RangerPolicy.RangerPolicyResource> policyResource) {
        RangerResourceDef ret = null;

        if(serviceDef != null && policyResource != null) {
            for(Map.Entry<String, RangerPolicy.RangerPolicyResource> entry : policyResource.entrySet()) {
                if (!isEmpty(entry.getValue())) {
                    String            resource    = entry.getKey();
                    RangerResourceDef resourceDef = ServiceDefUtil.getResourceDef(serviceDef, resource);

                    if (resourceDef != null && resourceDef.getLevel() != null) {
                        if (ret == null) {
                            ret = resourceDef;
                        } else if(ret.getLevel() < resourceDef.getLevel()) {
                            ret = resourceDef;
                        }
                    }
                }
            }
        }

        return ret;
    }

    public static boolean isEmpty(RangerPolicy.RangerPolicyResource policyResource) {
        boolean ret = true;
        if (policyResource != null) {
            List<String> resourceValues = policyResource.getValues();
            if (CollectionUtils.isNotEmpty(resourceValues)) {
                for (String resourceValue : resourceValues) {
                    if (StringUtils.isNotBlank(resourceValue)) {
                        ret = false;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public static String getOption(Map<String, String> options, String name, String defaultValue) {
        String ret = options != null && name != null ? options.get(name) : null;

        if(ret == null) {
            ret = defaultValue;
        }

        return ret;
    }

    public static boolean getBooleanOption(Map<String, String> options, String name, boolean defaultValue) {
        String val = getOption(options, name, null);

        return val == null ? defaultValue : Boolean.parseBoolean(val);
    }

    public static char getCharOption(Map<String, String> options, String name, char defaultValue) {
        String val = getOption(options, name, null);

        return StringUtils.isEmpty(val) ? defaultValue : val.charAt(0);
    }

    private static void normalizeDataMaskDef(RangerServiceDef serviceDef) {
        if(serviceDef != null && serviceDef.getDataMaskDef() != null) {
            List<RangerResourceDef>   dataMaskResources   = serviceDef.getDataMaskDef().getResources();
            List<RangerAccessTypeDef> dataMaskAccessTypes = serviceDef.getDataMaskDef().getAccessTypes();

            if(CollectionUtils.isNotEmpty(dataMaskResources)) {
                List<RangerResourceDef> resources     = serviceDef.getResources();
                List<RangerResourceDef> processedDefs = new ArrayList<RangerResourceDef>(dataMaskResources.size());

                for(RangerResourceDef dataMaskResource : dataMaskResources) {
                    RangerResourceDef processedDef = dataMaskResource;

                    for(RangerResourceDef resourceDef : resources) {
                        if(StringUtils.equals(resourceDef.getName(), dataMaskResource.getName())) {
                            processedDef = ServiceDefUtil.mergeResourceDef(resourceDef, dataMaskResource);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getDataMaskDef().setResources(processedDefs);
            }

            if(CollectionUtils.isNotEmpty(dataMaskAccessTypes)) {
                List<RangerAccessTypeDef> accessTypes   = serviceDef.getAccessTypes();
                List<RangerAccessTypeDef> processedDefs = new ArrayList<RangerAccessTypeDef>(accessTypes.size());

                for(RangerAccessTypeDef dataMaskAccessType : dataMaskAccessTypes) {
                    RangerAccessTypeDef processedDef = dataMaskAccessType;

                    for(RangerAccessTypeDef accessType : accessTypes) {
                        if(StringUtils.equals(accessType.getName(), dataMaskAccessType.getName())) {
                            processedDef = ServiceDefUtil.mergeAccessTypeDef(accessType, dataMaskAccessType);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getDataMaskDef().setAccessTypes(processedDefs);
            }
        }
    }

    private static void normalizeRowFilterDef(RangerServiceDef serviceDef) {
        if(serviceDef != null && serviceDef.getRowFilterDef() != null) {
            List<RangerResourceDef>   rowFilterResources   = serviceDef.getRowFilterDef().getResources();
            List<RangerAccessTypeDef> rowFilterAccessTypes = serviceDef.getRowFilterDef().getAccessTypes();

            if(CollectionUtils.isNotEmpty(rowFilterResources)) {
                List<RangerResourceDef> resources     = serviceDef.getResources();
                List<RangerResourceDef> processedDefs = new ArrayList<RangerResourceDef>(rowFilterResources.size());

                for(RangerResourceDef rowFilterResource : rowFilterResources) {
                    RangerResourceDef processedDef = rowFilterResource;

                    for(RangerResourceDef resourceDef : resources) {
                        if(StringUtils.equals(resourceDef.getName(), rowFilterResource.getName())) {
                            processedDef = ServiceDefUtil.mergeResourceDef(resourceDef, rowFilterResource);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getRowFilterDef().setResources(processedDefs);
            }

            if(CollectionUtils.isNotEmpty(rowFilterAccessTypes)) {
                List<RangerAccessTypeDef> accessTypes   = serviceDef.getAccessTypes();
                List<RangerAccessTypeDef> processedDefs = new ArrayList<RangerAccessTypeDef>(accessTypes.size());

                for(RangerAccessTypeDef rowFilterAccessType : rowFilterAccessTypes) {
                    RangerAccessTypeDef processedDef = rowFilterAccessType;

                    for(RangerAccessTypeDef accessType : accessTypes) {
                        if(StringUtils.equals(accessType.getName(), rowFilterAccessType.getName())) {
                            processedDef = ServiceDefUtil.mergeAccessTypeDef(accessType, rowFilterAccessType);
                            break;
                        }
                    }

                    processedDefs.add(processedDef);
                }

                serviceDef.getRowFilterDef().setAccessTypes(processedDefs);
            }
        }
    }

    private static RangerResourceDef mergeResourceDef(RangerResourceDef base, RangerResourceDef delta) {
        RangerResourceDef ret = new RangerResourceDef(base);

        // retain base values for: itemId, name, type, level, parent, lookupSupported

        if(Boolean.TRUE.equals(delta.getMandatory()))
            ret.setMandatory(delta.getMandatory());

        if(delta.getRecursiveSupported() != null)
            ret.setRecursiveSupported(delta.getRecursiveSupported());

        if(delta.getExcludesSupported() != null)
            ret.setExcludesSupported(delta.getExcludesSupported());

        if(StringUtils.isNotEmpty(delta.getMatcher()))
            ret.setMatcher(delta.getMatcher());

        if(MapUtils.isNotEmpty(delta.getMatcherOptions())) {
            if(ret.getMatcherOptions() == null) {
                ret.setMatcherOptions(new HashMap<String, String>());
            }

            for(Map.Entry<String, String> e : delta.getMatcherOptions().entrySet()) {
                ret.getMatcherOptions().put(e.getKey(), e.getValue());
            }
        }

        if(StringUtils.isNotEmpty(delta.getValidationRegEx()))
            ret.setValidationRegEx(delta.getValidationRegEx());

        if(StringUtils.isNotEmpty(delta.getValidationMessage()))
            ret.setValidationMessage(delta.getValidationMessage());

        ret.setUiHint(delta.getUiHint());

        if(StringUtils.isNotEmpty(delta.getLabel()))
            ret.setLabel(delta.getLabel());

        if(StringUtils.isNotEmpty(delta.getDescription()))
            ret.setDescription(delta.getDescription());

        if(StringUtils.isNotEmpty(delta.getRbKeyLabel()))
            ret.setRbKeyLabel(delta.getRbKeyLabel());

        if(StringUtils.isNotEmpty(delta.getRbKeyDescription()))
            ret.setRbKeyDescription(delta.getRbKeyDescription());

        if(StringUtils.isNotEmpty(delta.getRbKeyValidationMessage()))
            ret.setRbKeyValidationMessage(delta.getRbKeyValidationMessage());

        if(CollectionUtils.isNotEmpty(delta.getAccessTypeRestrictions()))
            ret.setAccessTypeRestrictions(delta.getAccessTypeRestrictions());

        if (delta.getIsValidLeaf() != null)
            ret.setIsValidLeaf(delta.getIsValidLeaf());

        return ret;
    }

    private static RangerAccessTypeDef mergeAccessTypeDef(RangerAccessTypeDef base, RangerAccessTypeDef delta) {
        RangerAccessTypeDef ret = new RangerAccessTypeDef(base);

        // retain base values for: itemId, name, impliedGrants

        if(StringUtils.isNotEmpty(delta.getLabel()))
            ret.setLabel(delta.getLabel());

        if(StringUtils.isNotEmpty(delta.getRbKeyLabel()))
            ret.setRbKeyLabel(delta.getRbKeyLabel());

        return ret;
    }

    private static boolean getBooleanValue(Map<String, String> map, String elementName, boolean defaultValue) {
        boolean ret = defaultValue;

        if(MapUtils.isNotEmpty(map) && map.containsKey(elementName)) {
            String elementValue = map.get(elementName);

            if(StringUtils.isNotEmpty(elementValue)) {
                ret = Boolean.valueOf(elementValue.toString());
            }
        }

        return ret;
    }

}
