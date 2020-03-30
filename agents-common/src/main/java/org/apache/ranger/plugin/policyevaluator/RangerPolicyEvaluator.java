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

package org.apache.ranger.plugin.policyevaluator;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyengine.RangerResourceAccessInfo;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceEvaluator;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;

import static org.apache.ranger.plugin.policyevaluator.RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW;
import static org.apache.ranger.plugin.policyevaluator.RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS;
import static org.apache.ranger.plugin.policyevaluator.RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY;
import static org.apache.ranger.plugin.policyevaluator.RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY_EXCEPTIONS;

public interface RangerPolicyEvaluator extends RangerPolicyResourceEvaluator {
	Comparator<RangerPolicyEvaluator> EVAL_ORDER_COMPARATOR = new RangerPolicyEvaluator.PolicyEvalOrderComparator();
	Comparator<RangerPolicyEvaluator> NAME_COMPARATOR       = new RangerPolicyEvaluator.PolicyNameComparator();

	// computation of PolicyACLSummary rely on following specific values
	Integer ACCESS_DENIED       = -1;
	Integer ACCESS_UNDETERMINED = 0;
	Integer ACCESS_ALLOWED      = 1;
	Integer ACCESS_CONDITIONAL  = 2;

	String EVALUATOR_TYPE_AUTO      = "auto";
	String EVALUATOR_TYPE_OPTIMIZED = "optimized";
	String EVALUATOR_TYPE_CACHED    = "cached";

	void init(RangerPolicy policy, RangerServiceDef serviceDef, RangerPolicyEngineOptions options);

	RangerPolicy getPolicy();

	RangerServiceDef getServiceDef();

	boolean hasAllow();

	boolean hasDeny();

	int getPolicyPriority();

	boolean isApplicable(Date accessTime);

	int getEvalOrder();

	long getUsageCount();

	void incrementUsageCount(int number);

	void setUsageCountImmutable();

	void resetUsageCount();

	int getCustomConditionsCount();

	int getValidityScheduleEvaluatorsCount();

	boolean isAuditEnabled();

	void evaluate(RangerAccessRequest request, RangerAccessResult result);

	boolean isMatch(RangerAccessResource resource, Map<String, Object> evalContext);

	boolean isCompleteMatch(RangerAccessResource resource, Map<String, Object> evalContext);

	boolean isCompleteMatch(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext);

	boolean isAccessAllowed(RangerAccessResource resource, String user, Set<String> userGroups, Set<String> roles, String accessType);

	boolean isAccessAllowed(Map<String, RangerPolicyResource> resources, String user, Set<String> userGroups, String accessType);

	boolean isAccessAllowed(RangerPolicy policy, String user, Set<String> userGroups, Set<String> roles, String accessType);

	void updateAccessResult(RangerAccessResult result, RangerPolicyResourceMatcher.MatchType matchType, boolean isAllowed, String reason);

	void getResourceAccessInfo(RangerAccessRequest request, RangerResourceAccessInfo result);

	PolicyACLSummary getPolicyACLSummary();

	default boolean hasContextSensitiveSpecification() {
		RangerPolicy policy = getPolicy();

		for (RangerPolicyItem policyItem : policy.getPolicyItems()) {
			if (hasContextSensitiveSpecification(policyItem)) {
				return true;
			}
		}
		for (RangerPolicyItem policyItem : policy.getDenyPolicyItems()) {
			if (hasContextSensitiveSpecification(policyItem)) {
				return true;
			}
		}
		for (RangerPolicyItem policyItem : policy.getAllowExceptions()) {
			if (hasContextSensitiveSpecification(policyItem)) {
				return true;
			}
		}
		for (RangerPolicyItem policyItem : policy.getDenyExceptions()) {
			if (hasContextSensitiveSpecification(policyItem)) {
				return true;
			}
		}
		return false;
	}

	default boolean hasRoles() {
		RangerPolicy policy = getPolicy();

		for (RangerPolicyItem policyItem : policy.getPolicyItems()) {
			if (hasRoles(policyItem)) {
				return true;
			}
		}
		for (RangerPolicyItem policyItem : policy.getDenyPolicyItems()) {
			if (hasRoles(policyItem)) {
				return true;
			}
		}
		for (RangerPolicyItem policyItem : policy.getAllowExceptions()) {
			if (hasRoles(policyItem)) {
				return true;
			}
		}
		for (RangerPolicyItem policyItem : policy.getDenyExceptions()) {
			if (hasRoles(policyItem)) {
				return true;
			}
		}
		for (RangerPolicy.RangerDataMaskPolicyItem policyItem : policy.getDataMaskPolicyItems()) {
			if (hasRoles(policyItem)) {
				return true;
			}
		}
		for (RangerPolicy.RangerRowFilterPolicyItem policyItem : policy.getRowFilterPolicyItems()) {
			if (hasRoles(policyItem)) {
				return true;
			}
		}
		return false;
	}

	static boolean hasContextSensitiveSpecification(RangerPolicyItem policyItem) {
		return  CollectionUtils.isNotEmpty(policyItem.getConditions()) || policyItem.getUsers().contains(RangerPolicyEngine.RESOURCE_OWNER); /* || policyItem.getGroups().contains(RangerPolicyEngine.RESOURCE_GROUP_OWNER) */
	}

	static boolean hasRoles(RangerPolicyItem policyItem) {
		return  CollectionUtils.isNotEmpty(policyItem.getRoles());
	}

	class PolicyEvalOrderComparator implements Comparator<RangerPolicyEvaluator>, Serializable {
		@Override
		public int compare(RangerPolicyEvaluator me, RangerPolicyEvaluator other) {
			int result = Integer.compare(other.getPolicyPriority(), me.getPolicyPriority());

			return result == 0 ? compareNormal(me, other) : result;
		}

		private int compareNormal(RangerPolicyEvaluator me, RangerPolicyEvaluator other) {
			int result;

			if (me.hasDeny() && !other.hasDeny()) {
				result = -1;
			} else if (!me.hasDeny() && other.hasDeny()) {
				result = 1;
			} else {
				result = Long.compare(other.getUsageCount(), me.getUsageCount());

				if (result == 0) {
					result = Integer.compare(me.getEvalOrder(), other.getEvalOrder());
				}
			}

			return result;
		}
	}

	class PolicyNameComparator implements Comparator<RangerPolicyEvaluator>, Serializable {
		@Override
		public int compare(RangerPolicyEvaluator me, RangerPolicyEvaluator other) {
			int result = Integer.compare(other.getPolicyPriority(), me.getPolicyPriority());

			return result == 0 ? compareNormal(me, other) : result;
		}

		private int compareNormal(RangerPolicyEvaluator me, RangerPolicyEvaluator other) {
			final int result;

			if (me.hasDeny() && !other.hasDeny()) {
				result = -1;
			} else if (!me.hasDeny() && other.hasDeny()) {
				result = 1;
			} else {
				result = me.getPolicy().getName().compareTo(other.getPolicy().getName());
			}

			return result;
		}
	}

	class PolicyACLSummary {
		private final Map<String, Map<String, AccessResult>> usersAccessInfo = new HashMap<>();
		private final Map<String, Map<String, AccessResult>> groupsAccessInfo = new HashMap<>();
		private final Map<String, Map<String, AccessResult>> rolesAccessInfo  = new HashMap<>();

		private enum AccessorType { USER, GROUP, ROLE }

		public static class AccessResult {
			private int result;
			private final boolean hasSeenDeny;

			public AccessResult(int result) {
				this(result, false);
			}

			public AccessResult(int result, boolean hasSeenDeny) {
				this.hasSeenDeny = hasSeenDeny;
				setResult(result);
			}

			public int getResult() {
				return result;
			}

			public void setResult(int result) {
				this.result = result;
			}

			public boolean getHasSeenDeny() {
				return hasSeenDeny;
			}
			@Override
			public String toString() {
				if (result == RangerPolicyEvaluator.ACCESS_ALLOWED)
					return "ALLOWED, hasSeenDeny=" + hasSeenDeny;
				if (result == RangerPolicyEvaluator.ACCESS_DENIED)
					return "NOT_ALLOWED, hasSeenDeny=" + hasSeenDeny;
				if (result == RangerPolicyEvaluator.ACCESS_CONDITIONAL)
					return "CONDITIONAL_ALLOWED, hasSeenDeny=" + hasSeenDeny;
				return "NOT_DETERMINED, hasSeenDeny=" + hasSeenDeny;
			}
		}

		PolicyACLSummary() {
		}

		public Map<String, Map<String, AccessResult>> getUsersAccessInfo() {
			return usersAccessInfo;
		}

		public Map<String, Map<String, AccessResult>> getGroupsAccessInfo() {
			return groupsAccessInfo;
		}

		public Map<String, Map<String, AccessResult>> getRolesAccessInfo() {
			return rolesAccessInfo;
		}

		void processPolicyItem(RangerPolicyItem policyItem, int policyItemType, boolean isConditional) {
			final Integer result;
			final boolean hasContextSensitiveSpecification = RangerPolicyEvaluator.hasContextSensitiveSpecification(policyItem);

			switch (policyItemType) {
				case POLICY_ITEM_TYPE_ALLOW:
					result = (hasContextSensitiveSpecification || isConditional) ? ACCESS_CONDITIONAL : ACCESS_ALLOWED;
					break;

				case POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS:
					result = (hasContextSensitiveSpecification || isConditional) ? null : ACCESS_DENIED;
					break;

				case POLICY_ITEM_TYPE_DENY:
					result = (hasContextSensitiveSpecification || isConditional) ? ACCESS_CONDITIONAL : ACCESS_DENIED;
					break;

				case POLICY_ITEM_TYPE_DENY_EXCEPTIONS:
					result = (hasContextSensitiveSpecification || isConditional) ? null : ACCESS_ALLOWED;
					break;

				default:
					result = null;
					break;
			}

			if (result != null) {
				final List<RangerPolicyItemAccess> accesses;

				if (policyItem.getDelegateAdmin()) {
					accesses = new ArrayList<>();

					accesses.add(new RangerPolicyItemAccess(RangerPolicyEngine.ADMIN_ACCESS, policyItem.getDelegateAdmin()));
					accesses.addAll(policyItem.getAccesses());
				} else {
					accesses = policyItem.getAccesses();
				}

				final List<String> groups         = policyItem.getGroups();
				final List<String> users          = policyItem.getUsers();
				final List<String> roles          = policyItem.getRoles();
				boolean            hasPublicGroup = false;

				for (RangerPolicyItemAccess access : accesses) {
					for (String user : users) {
						if (StringUtils.equals(user, RangerPolicyEngine.USER_CURRENT)) {
							hasPublicGroup = true;
							continue;
						} else if (StringUtils.isBlank(user)) {
							continue;
						}

						addAccess(user, AccessorType.USER, access.getType(), result, policyItemType);
					}

					for (String group : groups) {
						if (StringUtils.equals(group, RangerPolicyEngine.GROUP_PUBLIC)) {
							hasPublicGroup = true;
							continue;
						}

						addAccess(group, AccessorType.GROUP, access.getType(), result, policyItemType);
					}

					if (hasPublicGroup) {
						addAccess(RangerPolicyEngine.GROUP_PUBLIC, AccessorType.GROUP, access.getType(), result, policyItemType);
					}

					for (String role : roles) {
						addAccess(role, AccessorType.ROLE, access.getType(), result, policyItemType);
					}
				}
			}
		}

		void finalizeAcls(final boolean isDenyAllElse, final Set<String> allAccessTypeNames) {
			Map<String, AccessResult>  publicGroupAccessInfo = groupsAccessInfo.get(RangerPolicyEngine.GROUP_PUBLIC);

			if (publicGroupAccessInfo != null) {

				// For each accessType in public, retrieve access
				for (Map.Entry<String, AccessResult> entry : publicGroupAccessInfo.entrySet()) {
					final String       accessType   = entry.getKey();
					final AccessResult accessResult = entry.getValue();
					final int          access       = accessResult.getResult();

					if (access == ACCESS_DENIED || access == ACCESS_ALLOWED) {
						List<String> keysToRemove = null;

						for (Map.Entry<String, Map<String, AccessResult>> mapEntry : usersAccessInfo.entrySet()) {
							Map<String, AccessResult> mapValue = mapEntry.getValue();

							mapValue.remove(accessType);

							if (mapValue.isEmpty()) {
								if (keysToRemove == null) {
									keysToRemove = new ArrayList<>();
								}

								keysToRemove.add(mapEntry.getKey());
							}
						}

						if (keysToRemove != null) {
							for (String keyToRemove : keysToRemove) {
								usersAccessInfo.remove(keyToRemove);
							}

							keysToRemove.clear();
						}

						for (Map.Entry<String, Map<String, AccessResult>> mapEntry : groupsAccessInfo.entrySet()) {
							if (!StringUtils.equals(mapEntry.getKey(), RangerPolicyEngine.GROUP_PUBLIC)) {
								Map<String, AccessResult> mapValue = mapEntry.getValue();

								mapValue.remove(accessType);

								if (mapValue.isEmpty()) {
									if (keysToRemove == null) {
										keysToRemove = new ArrayList<>();
									}

									keysToRemove.add(mapEntry.getKey());
								}
							}
						}


						if (keysToRemove != null) {
							for (String keyToRemove : keysToRemove) {
								groupsAccessInfo.remove(keyToRemove);
							}

							keysToRemove.clear();
						}
					}
				}
			}

			if (isDenyAllElse) {
				// Go through all usersAccessInfo and groupsAccessInfo and mark ACCESS_UNDETERMINED to ACCESS_DENIED
				for (Map.Entry<String, Map<String, AccessResult>> mapEntry : usersAccessInfo.entrySet()) {
					for (Map.Entry<String, AccessResult> accessEntry : mapEntry.getValue().entrySet()) {
						AccessResult result = accessEntry.getValue();
						if (result.getResult() == ACCESS_UNDETERMINED) {
							result.setResult(ACCESS_DENIED);
						}
					}
				}

				for (Map.Entry<String, Map<String, AccessResult>> mapEntry : groupsAccessInfo.entrySet()) {
					for (Map.Entry<String, AccessResult> accessEntry : mapEntry.getValue().entrySet()) {
						AccessResult result = accessEntry.getValue();
						if (result.getResult() == ACCESS_UNDETERMINED) {
							result.setResult(ACCESS_DENIED);
						}
					}
				}

				// Mark all unseen accessTypeNames are having no permission
				for (Map.Entry<String, Map<String, AccessResult>> mapEntry : usersAccessInfo.entrySet()) {
					for (String accessTypeName : allAccessTypeNames) {
						if (!mapEntry.getValue().keySet().contains(accessTypeName)) {
							mapEntry.getValue().put(accessTypeName, new AccessResult(ACCESS_DENIED, true));
						}
					}
				}

				for (Map.Entry<String, Map<String, AccessResult>> mapEntry : groupsAccessInfo.entrySet()) {
					for (String accessTypeName : allAccessTypeNames) {
						if (!mapEntry.getValue().keySet().contains(accessTypeName)) {
							mapEntry.getValue().put(accessTypeName, new AccessResult(ACCESS_DENIED, true));
						}
					}
				}

				publicGroupAccessInfo = groupsAccessInfo.computeIfAbsent(RangerPolicyEngine.GROUP_PUBLIC, k -> new HashMap<>());

				Set<String> accessTypeNamesInPublicGroup = publicGroupAccessInfo.keySet();

				for (String accessTypeName : allAccessTypeNames) {
					if (!accessTypeNamesInPublicGroup.contains(accessTypeName)) {
						boolean isDenyAccess = true;
						for (Map.Entry<String, Map<String, AccessResult>> mapEntry : usersAccessInfo.entrySet()) {
							AccessResult result = mapEntry.getValue().get(accessTypeName);
							if (result == null || result.getResult() != ACCESS_DENIED) {
								isDenyAccess = false;
								break;
							}
						}
						if (isDenyAccess) {
							for (Map.Entry<String, Map<String, AccessResult>> mapEntry : groupsAccessInfo.entrySet()) {
								if (!StringUtils.equals(mapEntry.getKey(), RangerPolicyEngine.GROUP_PUBLIC)) {
									AccessResult result = mapEntry.getValue().get(accessTypeName);
									if (result == null || result.getResult() != ACCESS_DENIED) {
										isDenyAccess = false;
										break;
									}
								}
							}
						}
						publicGroupAccessInfo.put(accessTypeName, new AccessResult(isDenyAccess ? ACCESS_DENIED : ACCESS_CONDITIONAL, true));
					}
				}
			}
		}

		private void addAccess(String accessorName, AccessorType accessorType, String accessType, Integer access, int policyItemType) {
			final Map<String, Map<String, AccessResult>> accessorsAccessInfo;

			switch (accessorType) {
				case USER:
					accessorsAccessInfo = usersAccessInfo;
					break;

				case GROUP:
					accessorsAccessInfo = groupsAccessInfo;
					break;

				case ROLE:
					accessorsAccessInfo = rolesAccessInfo;
					break;

				default:
					return;
			}

			final Map<String, AccessResult> accessorAccessInfo = accessorsAccessInfo.computeIfAbsent(accessorName, k -> new HashMap<>());
			final AccessResult              currentAccess      = accessorAccessInfo.get(accessType);

			if (currentAccess == null) {
				if (policyItemType == POLICY_ITEM_TYPE_ALLOW || policyItemType == POLICY_ITEM_TYPE_DENY) {
					accessorAccessInfo.put(accessType, new AccessResult(access, policyItemType == POLICY_ITEM_TYPE_DENY));
				}
			} else {
				if (access.equals(RangerPolicyEvaluator.ACCESS_DENIED)) {
					if (currentAccess.getResult() == ACCESS_CONDITIONAL) {
						currentAccess.setResult(access);
					} else {
						int updatedAccessValue = currentAccess.getResult() + access;

						if (policyItemType == POLICY_ITEM_TYPE_DENY) {
							updatedAccessValue = (updatedAccessValue < ACCESS_DENIED) ? ACCESS_DENIED : updatedAccessValue;
						} else {
							updatedAccessValue = (updatedAccessValue < ACCESS_UNDETERMINED) ? ACCESS_UNDETERMINED : updatedAccessValue;
						}

						currentAccess.setResult(updatedAccessValue);
					}
				} else if (access.equals(RangerPolicyEvaluator.ACCESS_ALLOWED)) {
					if (currentAccess.getResult() == ACCESS_CONDITIONAL) {
						if (policyItemType == POLICY_ITEM_TYPE_ALLOW) {
							currentAccess.setResult(access);
						}
					} else {
						int     updatedAccessValue = currentAccess.getResult() + access;
						boolean replaceValue       = false;

						if (policyItemType == POLICY_ITEM_TYPE_ALLOW) {
							updatedAccessValue = (updatedAccessValue > ACCESS_ALLOWED) ? ACCESS_ALLOWED : updatedAccessValue;
							replaceValue       = true;            // Forget earlier stashed hasSeenDeny
						} else {
							updatedAccessValue = (updatedAccessValue > ACCESS_UNDETERMINED) ? ACCESS_UNDETERMINED : updatedAccessValue;
						}

						if (replaceValue) {
							accessorAccessInfo.put(accessType, new AccessResult(updatedAccessValue));
						} else {
							currentAccess.setResult(updatedAccessValue);
						}
					}
				} else {
					if (currentAccess.getResult() == ACCESS_UNDETERMINED) {
						currentAccess.setResult(access);
					}
				}
			}
		}
	}
}
