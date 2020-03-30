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


import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineImpl;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ServicePolicies implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private String             serviceName;
	private Long               serviceId;
	private Long               policyVersion;
	private Date               policyUpdateTime;
	private List<RangerPolicy> policies;
	private RangerServiceDef   serviceDef;
	private String             auditMode = RangerPolicyEngine.AUDIT_DEFAULT;
	private TagPolicies        tagPolicies;
	private Map<String, SecurityZoneInfo> securityZones;
	private List<RangerPolicyDelta> policyDeltas;
	private Map<String, Set<String>> userRoles;
	private Map<String, Set<String>> groupRoles;

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return serviceName;
	}
	/**
	 * @param serviceName the serviceName to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	/**
	 * @return the serviceId
	 */
	public Long getServiceId() {
		return serviceId;
	}
	/**
	 * @param serviceId the serviceId to set
	 */
	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}
	/**
	 * @return the policyVersion
	 */
	public Long getPolicyVersion() {
		return policyVersion;
	}
	/**
	 * @param policyVersion the policyVersion to set
	 */
	public void setPolicyVersion(Long policyVersion) {
		this.policyVersion = policyVersion;
	}
	/**
	 * @return the policyUpdateTime
	 */
	public Date getPolicyUpdateTime() {
		return policyUpdateTime;
	}
	/**
	 * @param policyUpdateTime the policyUpdateTime to set
	 */
	public void setPolicyUpdateTime(Date policyUpdateTime) {
		this.policyUpdateTime = policyUpdateTime;
	}
	/**
	 * @return the policies
	 */
	public List<RangerPolicy> getPolicies() {
		return policies;
	}
	/**
	 * @param policies the policies to set
	 */
	public void setPolicies(List<RangerPolicy> policies) {
		this.policies = policies;
	}
	/**
	 * @return the serviceDef
	 */
	public RangerServiceDef getServiceDef() {
		return serviceDef;
	}
	/**
	 * @param serviceDef the serviceDef to set
	 */
	public void setServiceDef(RangerServiceDef serviceDef) {
		this.serviceDef = serviceDef;
	}

	public String getAuditMode() {
		return auditMode;
	}

	public void setAuditMode(String auditMode) {
		this.auditMode = auditMode;
	}
	/**
	 * @return the tagPolicies
	 */
	public ServicePolicies.TagPolicies getTagPolicies() {
		return tagPolicies;
	}
	/**
	 * @param tagPolicies the tagPolicies to set
	 */
	public void setTagPolicies(ServicePolicies.TagPolicies tagPolicies) {
		this.tagPolicies = tagPolicies;
	}

	public Map<String, SecurityZoneInfo> getSecurityZones() { return securityZones; }

	public void setSecurityZones(Map<String, SecurityZoneInfo> securityZones) {
		this.securityZones = securityZones;
	}

	public Map<String, Set<String>> getUserRoles() { return userRoles; }
	public Map<String, Set<String>> getGroupRoles() { return groupRoles; }

	public void setUserRoles(Map<String, Set<String>> userRoles) { this.userRoles = userRoles; }
	public void setGroupRoles(Map<String, Set<String>> groupRoles) { this.groupRoles = groupRoles; }

	@Override
	public String toString() {
		return "serviceName=" + serviceName + ", "
				+ "serviceId=" + serviceId + ", "
			 	+ "policyVersion=" + policyVersion + ", "
			 	+ "policyUpdateTime=" + policyUpdateTime + ", "
			 	+ "policies=" + policies + ", "
			 	+ "tagPolicies=" + tagPolicies + ", "
			 	+ "policyDeltas=" + policyDeltas + ", "
			 	+ "serviceDef=" + serviceDef + ", "
			 	+ "auditMode=" + auditMode + ", "
			 	+ "securityZones=" + securityZones + ", "
				+ "userRoles=" + userRoles + ", "
				+ "groupRoles=" + groupRoles + ", "
				;
	}
	public List<RangerPolicyDelta> getPolicyDeltas() { return this.policyDeltas; }

	public void setPolicyDeltas(List<RangerPolicyDelta> policyDeltas) { this.policyDeltas = policyDeltas; }

	@JsonAutoDetect(fieldVisibility=Visibility.ANY)
	@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown=true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class TagPolicies implements java.io.Serializable {
		private static final long serialVersionUID = 1L;

		private String             serviceName;
		private Long               serviceId;
		private Long               policyVersion;
		private Date               policyUpdateTime;
		private List<RangerPolicy> policies;
		private RangerServiceDef   serviceDef;
		private String             auditMode = RangerPolicyEngine.AUDIT_DEFAULT;
		/**
		 * @return the serviceName
		 */
		public String getServiceName() {
			return serviceName;
		}
		/**
		 * @param serviceName the serviceName to set
		 */
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}
		/**
		 * @return the serviceId
		 */
		public Long getServiceId() {
			return serviceId;
		}
		/**
		 * @param serviceId the serviceId to set
		 */
		public void setServiceId(Long serviceId) {
			this.serviceId = serviceId;
		}
		/**
		 * @return the policyVersion
		 */
		public Long getPolicyVersion() {
			return policyVersion;
		}
		/**
		 * @param policyVersion the policyVersion to set
		 */
		public void setPolicyVersion(Long policyVersion) {
			this.policyVersion = policyVersion;
		}
		/**
		 * @return the policyUpdateTime
		 */
		public Date getPolicyUpdateTime() {
			return policyUpdateTime;
		}
		/**
		 * @param policyUpdateTime the policyUpdateTime to set
		 */
		public void setPolicyUpdateTime(Date policyUpdateTime) {
			this.policyUpdateTime = policyUpdateTime;
		}
		/**
		 * @return the policies
		 */
		public List<RangerPolicy> getPolicies() {
			return policies;
		}
		/**
		 * @param policies the policies to set
		 */
		public void setPolicies(List<RangerPolicy> policies) {
			this.policies = policies;
		}
		/**
		 * @return the serviceDef
		 */
		public RangerServiceDef getServiceDef() {
			return serviceDef;
		}
		/**
		 * @param serviceDef the serviceDef to set
		 */
		public void setServiceDef(RangerServiceDef serviceDef) {
			this.serviceDef = serviceDef;
		}

		public String getAuditMode() {
			return auditMode;
		}

		public void setAuditMode(String auditMode) {
			this.auditMode = auditMode;
		}

		@Override
		public String toString() {
			return "serviceName=" + serviceName + ", "
					+ "serviceId=" + serviceId + ", "
					+ "policyVersion=" + policyVersion + ", "
					+ "policyUpdateTime=" + policyUpdateTime + ", "
					+ "policies=" + policies + ", "
					+ "serviceDef=" + serviceDef + ", "
					+ "auditMode=" + auditMode
					;
		}
	}

	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SecurityZoneInfo implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		private String                          zoneName;
		private List<HashMap<String, List<String>>> resources;
		private List<RangerPolicy>              policies;
		private List<RangerPolicyDelta>         policyDeltas;
		private Boolean                         containsAssociatedTagService;

		public String getZoneName() {
			return zoneName;
		}

		public List<HashMap<String, List<String>>> getResources() {
			return resources;
		}

		public List<RangerPolicy> getPolicies() {
			return policies;
		}

		public List<RangerPolicyDelta> getPolicyDeltas() { return policyDeltas; }

		public Boolean getContainsAssociatedTagService() { return containsAssociatedTagService; }

		public void setZoneName(String zoneName) {
			this.zoneName = zoneName;
		}

		public void setResources(List<HashMap<String, List<String>>> resources) {
			this.resources = resources;
		}

		public void setPolicies(List<RangerPolicy> policies) {
			this.policies = policies;
		}

		public void setPolicyDeltas(List<RangerPolicyDelta> policyDeltas) { this.policyDeltas = policyDeltas; }

		public void setContainsAssociatedTagService(Boolean containsAssociatedTagService) { this.containsAssociatedTagService = containsAssociatedTagService; }

		@Override
		public String toString() {
			return "zoneName=" + zoneName + ", "
					+ "resources=" + resources + ", "
					+ "policies=" + policies + ", "
					+ "policyDeltas=" + policyDeltas + ", "
					+ "containsAssociatedTagService=" + containsAssociatedTagService
					;
		}
	}

	static public ServicePolicies copyHeader(ServicePolicies source) {
		ServicePolicies ret = new ServicePolicies();

		ret.setServiceName(source.getServiceName());
		ret.setServiceId(source.getServiceId());
		ret.setPolicyVersion(source.getPolicyVersion());
		ret.setAuditMode(source.getAuditMode());
		ret.setServiceDef(source.getServiceDef());
		ret.setPolicyUpdateTime(source.getPolicyUpdateTime());
		ret.setSecurityZones(source.getSecurityZones());
		ret.setUserRoles(source.getUserRoles());
		ret.setGroupRoles(source.getGroupRoles());
		ret.setPolicies(Collections.emptyList());
		ret.setPolicyDeltas(null);
		if (source.getTagPolicies() != null) {
			TagPolicies tagPolicies = copyHeader(source.getTagPolicies());
			ret.setTagPolicies(tagPolicies);
		}

		return ret;
	}

	static public TagPolicies copyHeader(TagPolicies source) {
		TagPolicies ret = new TagPolicies();

		ret.setServiceName(source.getServiceName());
		ret.setServiceId(source.getServiceId());
		ret.setPolicyVersion(source.getPolicyVersion());
		ret.setAuditMode(source.getAuditMode());
		ret.setServiceDef(source.getServiceDef());
		ret.setPolicyUpdateTime(source.getPolicyUpdateTime());
		ret.setPolicies(Collections.emptyList());

		return ret;
	}

	public static ServicePolicies applyDelta(final ServicePolicies servicePolicies, RangerPolicyEngineImpl policyEngine) {
		ServicePolicies ret = copyHeader(servicePolicies);

		List<RangerPolicy> oldResourcePolicies = policyEngine.getResourcePolicies();
		List<RangerPolicy> oldTagPolicies      = policyEngine.getTagPolicies();

		List<RangerPolicy> newResourcePolicies = RangerPolicyDeltaUtil.applyDeltas(oldResourcePolicies, servicePolicies.getPolicyDeltas(), servicePolicies.getServiceDef().getName());

		ret.setPolicies(newResourcePolicies);

		final List<RangerPolicy> newTagPolicies;
		if (servicePolicies.getTagPolicies() != null) {
			newTagPolicies = RangerPolicyDeltaUtil.applyDeltas(oldTagPolicies, servicePolicies.getPolicyDeltas(), servicePolicies.getTagPolicies().getServiceDef().getName());
		} else {
			newTagPolicies = null;
		}

		if (ret.getTagPolicies() != null) {
			ret.getTagPolicies().setPolicies(newTagPolicies);
		}

		if (MapUtils.isNotEmpty(servicePolicies.getSecurityZones())) {
			Map<String, SecurityZoneInfo> newSecurityZones = new HashMap<>();

			for (Map.Entry<String, SecurityZoneInfo> entry : servicePolicies.getSecurityZones().entrySet()) {
				String 			 zoneName = entry.getKey();
				SecurityZoneInfo zoneInfo = entry.getValue();

				List<RangerPolicy> zoneResourcePolicies = policyEngine.getResourcePolicies(zoneName);
				// There are no separate tag-policy-repositories for each zone

				final List<RangerPolicy> newZonePolicies = RangerPolicyDeltaUtil.applyDeltas(zoneResourcePolicies, zoneInfo.getPolicyDeltas(), servicePolicies.getServiceDef().getName());

				SecurityZoneInfo newZoneInfo = new SecurityZoneInfo();

				newZoneInfo.setZoneName(zoneName);
				newZoneInfo.setResources(zoneInfo.getResources());
				newZoneInfo.setPolicies(newZonePolicies);

				newSecurityZones.put(zoneName, newZoneInfo);
			}

			ret.setSecurityZones(newSecurityZones);
		}

		return ret;
	}
}
