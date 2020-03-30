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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.authorization.hadoop.config.RangerConfiguration;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceEvaluator;
import org.apache.ranger.plugin.resourcematcher.RangerAbstractResourceMatcher;
import org.apache.ranger.plugin.resourcematcher.RangerResourceMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RangerResourceTrie<T extends RangerPolicyResourceEvaluator> {
    private static final Log LOG = LogFactory.getLog(RangerResourceTrie.class);
    private static final Log TRACE_LOG = RangerPerfTracer.getPerfLogger("resourcetrie.trace");
    private static final Log PERF_TRIE_INIT_LOG = RangerPerfTracer.getPerfLogger("resourcetrie.init");
    private static final Log PERF_TRIE_OP_LOG = RangerPerfTracer.getPerfLogger("resourcetrie.op");

    private static final String DEFAULT_WILDCARD_CHARS = "*?";
    private static final String TRIE_BUILDER_THREAD_COUNT = "ranger.policyengine.trie.builder.thread.count";

    private final String resourceName;
    private final boolean optIgnoreCase;
    private final boolean optWildcard;
    private final String wildcardChars;
    private final TrieNode<T> root;
    private final Comparator<T> comparator;
    private final boolean isOptimizedForRetrieval;

    public RangerResourceTrie(RangerServiceDef.RangerResourceDef resourceDef, List<T> evaluators) {
        this(resourceDef, evaluators, null, true);
    }

    public RangerResourceTrie(RangerServiceDef.RangerResourceDef resourceDef, List<T> evaluators, Comparator<T> comparator, boolean isOptimizedForRetrieval) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerResourceTrie(" + resourceDef.getName() + ", evaluatorCount=" + evaluators.size() + ", isOptimizedForRetrieval=" + isOptimizedForRetrieval + ")");
        }

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.init(name=" + resourceDef.getName() + ")");
        }

        int builderThreadCount = RangerConfiguration.getInstance().getInt(TRIE_BUILDER_THREAD_COUNT, 1);

        if (builderThreadCount < 1) {
            builderThreadCount = 1;
        }

        if (TRACE_LOG.isTraceEnabled()) {
            TRACE_LOG.trace("builderThreadCount is set to [" + builderThreadCount + "]");
        }

        Map<String, String> matcherOptions = resourceDef.getMatcherOptions();

        boolean optReplaceTokens = RangerAbstractResourceMatcher.getOptionReplaceTokens(matcherOptions);

        String tokenReplaceSpecialChars = "";

        if(optReplaceTokens) {
            char delimiterStart  = RangerAbstractResourceMatcher.getOptionDelimiterStart(matcherOptions);
            char delimiterEnd    = RangerAbstractResourceMatcher.getOptionDelimiterEnd(matcherOptions);
            char delimiterEscape = RangerAbstractResourceMatcher.getOptionDelimiterEscape(matcherOptions);

            tokenReplaceSpecialChars += delimiterStart;
            tokenReplaceSpecialChars += delimiterEnd;
            tokenReplaceSpecialChars += delimiterEscape;
        }

        this.resourceName  = resourceDef.getName();
        this.optIgnoreCase = RangerAbstractResourceMatcher.getOptionIgnoreCase(matcherOptions);
        this.optWildcard   = RangerAbstractResourceMatcher.getOptionWildCard(matcherOptions);
        this.wildcardChars = optWildcard ? DEFAULT_WILDCARD_CHARS + tokenReplaceSpecialChars : "" + tokenReplaceSpecialChars;
        this.comparator    = comparator;
        this.isOptimizedForRetrieval = isOptimizedForRetrieval;

        TrieNode<T> tmpRoot = buildTrie(resourceDef, evaluators, comparator, builderThreadCount);

        if (builderThreadCount > 1 && tmpRoot == null) { // if multi-threaded trie-creation failed, build using a single thread
            this.root = buildTrie(resourceDef, evaluators, comparator, 1);
        } else {
            this.root = tmpRoot;
        }

        RangerPerfTracer.logAlways(perf);

        if (PERF_TRIE_INIT_LOG.isDebugEnabled()) {
            PERF_TRIE_INIT_LOG.debug(toString());
        }

        if (TRACE_LOG.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            root.toString("", sb);
            TRACE_LOG.trace("Trie Dump from RangerResourceTrie.init(name=" + resourceName + "):\n{" + sb.toString() + "}");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerResourceTrie(" + resourceDef.getName() + ", evaluatorCount=" + evaluators.size() + ", isOptimizedForRetrieval=" + isOptimizedForRetrieval + "): " + toString());
        }
    }

    public String getResourceName() {
        return resourceName;
    }

    public List<T> getEvaluatorsForResource(Object resource) {
        if (resource instanceof String) {
            return getEvaluatorsForResource((String) resource);
        } else if (resource instanceof Collection) {
            if (CollectionUtils.isEmpty((Collection) resource)) {  // treat empty collection same as empty-string
                return getEvaluatorsForResource("");
            } else {
                @SuppressWarnings("unchecked")
                Collection<String> resources = (Collection<String>) resource;

                return getEvaluatorsForResources(resources);
            }
        }

        return null;
    }

    public void add(RangerPolicyResource resource, T evaluator) {

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.add(name=" + resource + ")");
        }

        if (resource.getIsExcludes()) {
            root.addWildcardEvaluator(evaluator);
        } else {
            if (CollectionUtils.isNotEmpty(resource.getValues())) {
                for (String value : resource.getValues()) {
                    insert(root, value, resource.getIsRecursive(), evaluator);
                }
            }
        }

        RangerPerfTracer.logAlways(perf);
        if (TRACE_LOG.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            root.toString("", sb);
            TRACE_LOG.trace("Trie Dump from RangerResourceTrie.add(name=" + resource + "):\n{" + sb.toString() + "}");
        }
    }

    public void delete(RangerPolicyResource resource, T evaluator) {

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.delete(name=" + resource + ")");
        }

        boolean isRemoved = false;
        if (resource.getIsExcludes()) {
            if (CollectionUtils.isNotEmpty(root.wildcardEvaluators)) {
                isRemoved = root.wildcardEvaluators.remove(evaluator);
                if (isRemoved && CollectionUtils.isEmpty(root.wildcardEvaluators)) {
                    root.wildcardEvaluators = null;
                }
            }
        }
        if (!isRemoved) {
            for (String value : resource.getValues()) {
                TrieNode<T> node = getNodeForResource(value);
                if (node != null) {
                    node.removeEvaluatorFromSubtree(evaluator);
                }
            }
        }

        RangerPerfTracer.logAlways(perf);
        if (TRACE_LOG.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            root.toString("", sb);
            TRACE_LOG.trace("Trie Dump from RangerResourceTrie.delete(name=" + resource + "):\n{" + sb.toString() + "}");
        }
    }

    public void wrapUpUpdate() {
        if (this.isOptimizedForRetrieval) {
            root.postSetup(null, comparator);
        } else {
            root.setup(null, comparator);
        }
    }

    private TrieNode<T> copyTrieSubtree(TrieNode<T> source, List<T> parentWildcardEvaluators) {
        if (TRACE_LOG.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            source.toString(sb);
            TRACE_LOG.trace("==> copyTrieSubtree(" + sb + ", parentWildCardEvaluators=" + (parentWildcardEvaluators != null ? Arrays.toString(parentWildcardEvaluators.toArray()) : "[]") + ")");
        }
        TrieNode<T> dest = new TrieNode<>(source.str);
        boolean setUpCompleted = source.isSetup;
        if (!setUpCompleted) {
            synchronized (source) {
                setUpCompleted = source.isSetup;
                if (!setUpCompleted) {
                    if (source.wildcardEvaluators != null) {
                        dest.wildcardEvaluators = new ArrayList<>(source.wildcardEvaluators);
                    } else {
                        dest.wildcardEvaluators = null;
                    }
                    if (source.evaluators != null) {
                        if (source.evaluators == source.wildcardEvaluators) {
                            dest.evaluators = null;
                        } else {
                            dest.evaluators = new ArrayList<>(source.evaluators);
                        }
                    } else {
                        dest.evaluators = null;
                    }
                }
            }
        }
        if (setUpCompleted) {
            if (source.isSharingParentWildcardEvaluators) {
                dest.wildcardEvaluators = null;
            } else {
                if (source.wildcardEvaluators != null) {
                    dest.wildcardEvaluators = new ArrayList<>(source.wildcardEvaluators);
                    if (parentWildcardEvaluators != null) {
                        dest.wildcardEvaluators.removeAll(parentWildcardEvaluators);
                    }
                } else {
                    dest.wildcardEvaluators = null;
                }
            }
            if (source.evaluators != null) {
                if (source.evaluators == source.wildcardEvaluators) {
                    dest.evaluators = null;
                } else {
                    dest.evaluators = new ArrayList<>(source.evaluators);
                    if (source.wildcardEvaluators != null) {
                        dest.evaluators.removeAll(source.wildcardEvaluators);
                    }
                }
            } else {
                dest.evaluators = null;
            }
        }

        Map<Character, TrieNode<T>> children = source.getChildren();
        for (Map.Entry<Character, TrieNode<T>> entry : children.entrySet()) {
            TrieNode<T> copy = copyTrieSubtree(entry.getValue(), source.wildcardEvaluators);
            dest.addChild(copy);
        }

        if (TRACE_LOG.isTraceEnabled()) {
            StringBuilder sourceAsString = new StringBuilder(), destAsString = new StringBuilder();
            source.toString(sourceAsString);
            dest.toString(destAsString);

            TRACE_LOG.trace("<== copyTrieSubtree(" + sourceAsString + ", parentWildCardEvaluators=" + (parentWildcardEvaluators != null ? Arrays.toString(parentWildcardEvaluators.toArray()) : "[]") + ") : " + destAsString);
        }
        return dest;
    }

    public RangerResourceTrie(RangerResourceTrie<T> other) {
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.copyTrie(name=" + other.resourceName + ")");
        }

        this.resourceName = other.resourceName;
        this.optIgnoreCase = other.optIgnoreCase;
        this.optWildcard = other.optWildcard;
        this.wildcardChars = other.wildcardChars;
        this.comparator = other.comparator;
        this.isOptimizedForRetrieval = other.isOptimizedForRetrieval;
        this.root = copyTrieSubtree(other.root, null);

        RangerPerfTracer.logAlways(perf);

        if (PERF_TRIE_INIT_LOG.isDebugEnabled()) {
            PERF_TRIE_INIT_LOG.debug(toString());
        }
        if (TRACE_LOG.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            root.toString("", sb);
            TRACE_LOG.trace("Trie Dump from RangerResourceTrie.copyTrie(name=" + other.resourceName + "):\n{" + sb.toString() + "}");
        }
    }

    private TrieNode<T> buildTrie(RangerServiceDef.RangerResourceDef resourceDef, List<T> evaluators, Comparator<T> comparator, int builderThreadCount) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> buildTrie(" + resourceDef.getName() + ", evaluatorCount=" + evaluators.size() + ", isMultiThreaded=" + (builderThreadCount > 1) + ")");
        }

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.init(resourceDef=" + resourceDef.getName() + ")");
        }

        TrieNode<T>                           ret                 = new TrieNode<>(null);
        final boolean                         isMultiThreaded = builderThreadCount > 1;
        final List<ResourceTrieBuilderThread> builderThreads;
        final Map<Character, Integer>         builderThreadMap;
        int                                   lastUsedThreadIndex = 0;

        if (isMultiThreaded) {
            builderThreads = new ArrayList<>();
            for (int i = 0; i < builderThreadCount; i++) {
                ResourceTrieBuilderThread t = new ResourceTrieBuilderThread(isOptimizedForRetrieval);
                t.setDaemon(true);
                builderThreads.add(t);
                t.start();
            }
            builderThreadMap = new HashMap<>();
        } else {
            builderThreads = null;
            builderThreadMap = null;
        }

        for (T evaluator : evaluators) {
            Map<String, RangerPolicyResource> policyResources = evaluator.getPolicyResource();
            RangerPolicyResource policyResource = policyResources != null ? policyResources.get(resourceName) : null;

            if (policyResource == null) {
                if (evaluator.getLeafResourceLevel() != null && resourceDef.getLevel() != null && evaluator.getLeafResourceLevel() < resourceDef.getLevel()) {
                    ret.addWildcardEvaluator(evaluator);
                }

                continue;
            }

            if (policyResource.getIsExcludes()) {
                ret.addWildcardEvaluator(evaluator);
            } else {
                RangerResourceMatcher resourceMatcher = evaluator.getResourceMatcher(resourceName);

                if (resourceMatcher != null && (resourceMatcher.isMatchAny())) {
                    ret.addWildcardEvaluator(evaluator);
                } else {
                    if (CollectionUtils.isNotEmpty(policyResource.getValues())) {
                        for (String resource : policyResource.getValues()) {
                            if (!isMultiThreaded) {
                                insert(ret, resource, policyResource.getIsRecursive(), evaluator);
                            } else {
                                try {
                                    lastUsedThreadIndex = insert(ret, resource, policyResource.getIsRecursive(), evaluator, builderThreadMap, builderThreads, lastUsedThreadIndex);
                                } catch (InterruptedException ex) {
                                    LOG.error("Failed to dispatch " + resource + " to " + builderThreads.get(lastUsedThreadIndex));
                                    LOG.error("Failing and retrying with one thread");

                                    ret = null;

                                    break;
                                }
                            }
                        }
                        if (ret == null) {
                            break;
                        }
                    }
                }
            }
        }
        if (ret != null) {
            if (isMultiThreaded) {
                ret.setup(null, comparator);

                for (ResourceTrieBuilderThread t : builderThreads) {
                    t.setParentWildcardEvaluators(ret.wildcardEvaluators);
                    try {
                        // Send termination signal to each thread
                        t.add("", false, null);
                        // Wait for threads to finish work
                        t.join();
                        ret.getChildren().putAll(t.getSubtrees());
                    } catch (InterruptedException ex) {
                        LOG.error("BuilderThread " + t + " was interrupted:", ex);
                        LOG.error("Failing and retrying with one thread");

                        ret = null;

                        break;
                    }
                }
            } else {
                if (isOptimizedForRetrieval) {
                    RangerPerfTracer postSetupPerf = null;

                    if (RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
                        postSetupPerf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.init(name=" + resourceDef.getName() + "-postSetup)");
                    }

                    ret.postSetup(null, comparator);

                    RangerPerfTracer.logAlways(postSetupPerf);
                } else {
                    ret.setup(null, comparator);
                }
            }
        }

        if (isMultiThreaded) {
            cleanUpThreads(builderThreads);
        }

        RangerPerfTracer.logAlways(perf);

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== buildTrie(" + resourceDef.getName() + ", evaluatorCount=" + evaluators.size() + ", isMultiThreaded=" + isMultiThreaded + ") :" +  ret);
        }

        return ret;
    }

    private void cleanUpThreads(List<ResourceTrieBuilderThread> builderThreads) {
        if (CollectionUtils.isNotEmpty(builderThreads)) {
            for (ResourceTrieBuilderThread t : builderThreads) {
                try {
                    if (t.isAlive()) {
                        t.interrupt();
                        t.join();
                    }
                } catch (InterruptedException ex) {
                    LOG.error("Could not terminate thread " + t);
                }
            }
        }
    }

    private TrieData getTrieData() {
        TrieData ret = new TrieData();

        root.populateTrieData(ret);
        ret.maxDepth = getMaxDepth();

        return ret;
    }

    private int getMaxDepth() {
        return root.getMaxDepth();
    }

    private Character getLookupChar(char ch) {
        return optIgnoreCase ? Character.toLowerCase(ch) : ch;
    }

    private Character getLookupChar(String str, int index) {
        return getLookupChar(str.charAt(index));
    }

    private int insert(TrieNode<T> currentRoot, String resource, boolean isRecursive, T evaluator, Map<Character, Integer> builderThreadMap, List<ResourceTrieBuilderThread> builderThreads, int lastUsedThreadIndex) throws InterruptedException {
        int          ret    = lastUsedThreadIndex;
        final String prefix = getNonWildcardPrefix(resource);

        if (StringUtils.isNotEmpty(prefix)) {
            char    c     = getLookupChar(prefix.charAt(0));
            Integer index = builderThreadMap.get(c);

            if (index == null) {
                ret = index = (lastUsedThreadIndex + 1) % builderThreads.size();
                builderThreadMap.put(c, index);
            }

            builderThreads.get(index).add(resource, isRecursive, evaluator);
        } else {
            currentRoot.addWildcardEvaluator(evaluator);
        }

        return ret;
    }

    private void insert(TrieNode<T> currentRoot, String resource, boolean isRecursive, T evaluator) {

        TrieNode<T>   curr       = currentRoot;
        final String  prefix     = getNonWildcardPrefix(resource);
        final boolean isWildcard = prefix.length() != resource.length();

        if (StringUtils.isNotEmpty(prefix)) {
            curr = curr.getOrCreateChild(prefix);
        }

        if(isWildcard || isRecursive) {
            curr.addWildcardEvaluator(evaluator);
        } else {
            curr.addEvaluator(evaluator);
        }

    }

    private String getNonWildcardPrefix(String str) {

        int minIndex = str.length();

        for (int i = 0; i < wildcardChars.length(); i++) {
            int index = str.indexOf(wildcardChars.charAt(i));

            if (index != -1 && index < minIndex) {
                minIndex = index;
            }
        }

        return str.substring(0, minIndex);
    }

    private List<T> getEvaluatorsForResource(String resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerResourceTrie.getEvaluatorsForResource(" + resource + ")");
        }

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_OP_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_OP_LOG, "RangerResourceTrie.getEvaluatorsForResource(resource=" + resource + ")");
        }

        TrieNode<T> curr   = root;
        TrieNode<T> parent = null;
        final int   len    = resource.length();
        int         i      = 0;

        while (i < len) {
            if (!isOptimizedForRetrieval) {
                curr.setupIfNeeded(parent, comparator);
            }

            final TrieNode<T> child = curr.getChild(getLookupChar(resource, i));

            if (child == null) {
                break;
            }

            final String childStr = child.getStr();

            if (!resource.regionMatches(optIgnoreCase, i, childStr, 0, childStr.length())) {
                break;
            }

            parent = curr;
            curr = child;
            i += childStr.length();
        }

        if (!isOptimizedForRetrieval) {
            curr.setupIfNeeded(parent, comparator);
        }

        List<T> ret = i == len ? curr.getEvaluators() : curr.getWildcardEvaluators();

        RangerPerfTracer.logAlways(perf);

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerResourceTrie.getEvaluatorsForResource(" + resource + "): evaluatorCount=" + (ret == null ? 0 : ret.size()));
        }

        return ret;
    }

    private TrieNode<T> getNodeForResource(String resource) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerResourceTrie.getNodeForResource(" + resource + ")");
        }

        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_OP_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TRIE_OP_LOG, "RangerResourceTrie.getNodeForResource(resource=" + resource + ")");
        }

        TrieNode<T> curr   = root;
        final int   len    = resource.length();
        int         i      = 0;

        while (i < len) {

            final TrieNode<T> child = curr.getChild(getLookupChar(resource, i));

            if (child == null) {
                break;
            }

            final String childStr = child.getStr();

            if (!resource.regionMatches(optIgnoreCase, i, childStr, 0, childStr.length())) {
                break;
            }

            curr = child;
            i += childStr.length();
        }

        RangerPerfTracer.logAlways(perf);

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerResourceTrie.getNodeForResource(" + resource + ")");
        }

        return curr;
    }

    private List<T> getEvaluatorsForResources(Collection<String> resources) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerResourceTrie.getEvaluatorsForResources(" + resources + ")");
        }

        List<T>      ret           = null;
        Map<Long, T> evaluatorsMap = null;

        for (String resource : resources) {
            List<T> resourceEvaluators = getEvaluatorsForResource(resource);

            if (CollectionUtils.isEmpty(resourceEvaluators)) {
                continue;
            }

            if (evaluatorsMap == null) {
                if (ret == null) { // first resource: don't create map yet
                    ret = resourceEvaluators;
                } else if (ret != resourceEvaluators) { // if evaluator list is same as earlier resources, retain the list, else create a map
                    evaluatorsMap = new HashMap<>();

                    for (T evaluator : ret) {
                        evaluatorsMap.put(evaluator.getId(), evaluator);
                    }

                    ret = null;
                }
            }

            if (evaluatorsMap != null) {
                for (T evaluator : resourceEvaluators) {
                    evaluatorsMap.put(evaluator.getId(), evaluator);
                }
            }
        }

        if (ret == null && evaluatorsMap != null) {
            ret = new ArrayList<>(evaluatorsMap.values());

            if (comparator != null) {
                ret.sort(comparator);
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerResourceTrie.getEvaluatorsForResources(" + resources + "): evaluatorCount=" + (ret == null ? 0 : ret.size()));
        }

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        TrieData trieData = getTrieData();

        sb.append("resourceName=").append(resourceName);
        sb.append("; optIgnoreCase=").append(optIgnoreCase);
        sb.append("; optWildcard=").append(optWildcard);
        sb.append("; wildcardChars=").append(wildcardChars);
        sb.append("; nodeCount=").append(trieData.nodeCount);
        sb.append("; leafNodeCount=").append(trieData.leafNodeCount);
        sb.append("; singleChildNodeCount=").append(trieData.singleChildNodeCount);
        sb.append("; maxDepth=").append(trieData.maxDepth);
        sb.append("; evaluatorListCount=").append(trieData.evaluatorListCount);
        sb.append("; wildcardEvaluatorListCount=").append(trieData.wildcardEvaluatorListCount);
        sb.append("; evaluatorListRefCount=").append(trieData.evaluatorListRefCount);
        sb.append("; wildcardEvaluatorListRefCount=").append(trieData.wildcardEvaluatorListRefCount);

        return sb.toString();
    }

    class ResourceTrieBuilderThread extends Thread {

        class WorkItem {
            final String  resourceName;
            final boolean isRecursive;
            final T       evaluator;

            WorkItem(String resourceName, boolean isRecursive, T evaluator) {
                this.resourceName   = resourceName;
                this.isRecursive    = isRecursive;
                this.evaluator      = evaluator;
            }
            @Override
            public String toString() {
                return
                "resourceName=" + resourceName +
                "isRecursive=" + isRecursive +
                "evaluator=" + (evaluator != null? evaluator.getId() : null);
            }
        }

        private final   TrieNode<T>             thisRoot  = new TrieNode<>(null);
        private final   BlockingQueue<WorkItem> workQueue = new LinkedBlockingQueue<>();
        private final   boolean                 isOptimizedForRetrieval;
        private         List<T>                 parentWildcardEvaluators;

        ResourceTrieBuilderThread(boolean isOptimizedForRetrieval) {
            this.isOptimizedForRetrieval = isOptimizedForRetrieval;
        }

        void add(String resourceName, boolean isRecursive, T evaluator) throws InterruptedException {
            workQueue.put(new WorkItem(resourceName, isRecursive, evaluator));
        }

        void setParentWildcardEvaluators(List<T> parentWildcardEvaluators) {
            this.parentWildcardEvaluators = parentWildcardEvaluators;
        }

        Map<Character, TrieNode<T>> getSubtrees() { return thisRoot.getChildren(); }

        @Override
        public void run() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Running " + this);
            }

            while (true) {
                final WorkItem workItem;

                try {
                    workItem = workQueue.take();
                } catch (InterruptedException exception) {
                    LOG.error("Thread=" + this + " is interrupted", exception);

                    break;
                }

                if (workItem.evaluator != null) {
                    insert(thisRoot, workItem.resourceName, workItem.isRecursive, workItem.evaluator);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Received termination signal. " + workItem);
                    }
                    break;
                }
            }

            if (!isInterrupted() && isOptimizedForRetrieval) {
                RangerPerfTracer postSetupPerf = null;

                if (RangerPerfTracer.isPerfTraceEnabled(PERF_TRIE_INIT_LOG)) {
                    postSetupPerf = RangerPerfTracer.getPerfTracer(PERF_TRIE_INIT_LOG, "RangerResourceTrie.init(thread=" + this.getName() + "-postSetup)");
                }

                thisRoot.postSetup(parentWildcardEvaluators, comparator);

                RangerPerfTracer.logAlways(postSetupPerf);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Exiting " + this);
            }
        }
    }

    class TrieData {
        int nodeCount;
        int leafNodeCount;
        int singleChildNodeCount;
        int maxDepth;
        int evaluatorListCount;
        int wildcardEvaluatorListCount;
        int evaluatorListRefCount;
        int wildcardEvaluatorListRefCount;
    }

    class TrieNode<U extends RangerPolicyResourceEvaluator> {
        private          String                      str;
        private final    Map<Character, TrieNode<U>> children = new HashMap<>();
        private          List<U>                     evaluators;
        private          List<U>                     wildcardEvaluators;
        private          boolean                     isSharingParentWildcardEvaluators;
        private volatile boolean                     isSetup = false;

        TrieNode(String str) {
            this.str = str;
        }

        String getStr() {
            return str;
        }

        void setStr(String str) {
            this.str = str;
        }

        Map<Character, TrieNode<U>> getChildren() {
            return children;
        }

        List<U> getEvaluators() {
            return evaluators;
        }

        List<U> getWildcardEvaluators() {
            return wildcardEvaluators;
        }

        TrieNode<U> getChild(Character ch) {
            return children == null ? null : children.get(ch);
        }

        void populateTrieData(RangerResourceTrie.TrieData trieData) {
            trieData.nodeCount++;

            if (wildcardEvaluators != null) {
                if (isSharingParentWildcardEvaluators) {
                    trieData.wildcardEvaluatorListRefCount++;
                } else {
                    trieData.wildcardEvaluatorListCount++;
                }
            }

            if (evaluators != null) {
                if (evaluators == wildcardEvaluators) {
                    trieData.evaluatorListRefCount++;
                } else {
                    trieData.evaluatorListCount++;
                }
            }

            if (children != null && !children.isEmpty()) {
                if (children.size() == 1) {
                    trieData.singleChildNodeCount++;
                }

                for (Map.Entry<Character, TrieNode<U>> entry : children.entrySet()) {
                    TrieNode child = entry.getValue();

                    child.populateTrieData(trieData);
                }
            } else {
                trieData.leafNodeCount++;
            }
        }

        int getMaxDepth() {
            int ret = 0;

            if (children != null) {
                for (Map.Entry<Character, TrieNode<U>> entry : children.entrySet()) {
                    TrieNode<U> child = entry.getValue();

                    int maxChildDepth = child.getMaxDepth();

                    if (maxChildDepth > ret) {
                        ret = maxChildDepth;
                    }
                }
            }

            return ret + 1;
        }

        TrieNode<U> getOrCreateChild(String str) {
            int len = str.length();

            TrieNode<U> child = children.get(getLookupChar(str, 0));

            if (child == null) {
                child = new TrieNode<>(str);
                addChild(child);
            } else {
                final String childStr = child.getStr();
                final int childStrLen = childStr.length();

                final boolean isExactMatch = optIgnoreCase ? StringUtils.equalsIgnoreCase(childStr, str) : StringUtils.equals(childStr, str);

                if (!isExactMatch) {
                    final int numOfCharactersToMatch = childStrLen < len ? childStrLen : len;
                    int index = 1;
                    for (; index < numOfCharactersToMatch; index++) {
                        if (getLookupChar(childStr, index) != getLookupChar(str, index)) {
                            break;
                        }
                    }
                    if (index == numOfCharactersToMatch) {
                        // Matched all
                        if (childStrLen > len) {
                            // Existing node has longer string, need to break up this node
                            TrieNode<U> newChild = new TrieNode<>(str);
                            this.addChild(newChild);
                            child.setStr(childStr.substring(index));
                            newChild.addChild(child);
                            child = newChild;
                        } else {
                            // This is a longer string, build a child with leftover string
                            child = child.getOrCreateChild(str.substring(index));
                        }
                    } else {
                        // Partial match for both; both have leftovers
                        String matchedPart = str.substring(0, index);
                        TrieNode<U> newChild = new TrieNode<>(matchedPart);
                        this.addChild(newChild);
                        child.setStr(childStr.substring(index));
                        newChild.addChild(child);
                        child = newChild.getOrCreateChild(str.substring(index));
                    }
                }
            }

            return child;
        }

        private void addChild(TrieNode<U> child) {
            children.put(getLookupChar(child.getStr(), 0), child);
        }

        void addEvaluator(U evaluator) {
            if (evaluators == null) {
                evaluators = new ArrayList<>();
            }

            if (!evaluators.contains(evaluator)) {
                evaluators.add(evaluator);
            }
        }

        void addWildcardEvaluator(U evaluator) {
            if (wildcardEvaluators == null) {
                wildcardEvaluators = new ArrayList<>();
            }

            if (!wildcardEvaluators.contains(evaluator)) {
                wildcardEvaluators.add(evaluator);
            }
        }

        void postSetup(List<U> parentWildcardEvaluators, Comparator<U> comparator) {

            setup(parentWildcardEvaluators, comparator);

            if (children != null) {
                for (Map.Entry<Character, TrieNode<U>> entry : children.entrySet()) {
                    TrieNode<U> child = entry.getValue();

                    child.postSetup(wildcardEvaluators, comparator);
                }
            }
        }

        void setupIfNeeded(TrieNode<U> parent, Comparator<U> comparator) {
            if (parent == null) {
                return;
            }

            boolean setupNeeded = !isSetup;

            if (setupNeeded) {
                synchronized (this) {
                    setupNeeded = !isSetup;

                    if (setupNeeded) {
                        setup(parent.getWildcardEvaluators(), comparator);
                        isSetup = true;
                        if (TRACE_LOG.isTraceEnabled()) {
                            StringBuilder sb = new StringBuilder();
                            this.toString(sb);
                            TRACE_LOG.trace("Set up is completed for this TriNode as a part of access evaluation : [" + sb + "]");
                        }
                    }
                }
            }
        }

        void setup(List<U> parentWildcardEvaluators, Comparator<U> comparator) {
            // finalize wildcard-evaluators list by including parent's wildcard evaluators
            if (parentWildcardEvaluators != null) {
                if (CollectionUtils.isEmpty(this.wildcardEvaluators)) {
                    this.wildcardEvaluators = parentWildcardEvaluators;
                } else {
                    for (U evaluator : parentWildcardEvaluators) {
                        addWildcardEvaluator(evaluator);
                    }
                }
            }
            this.isSharingParentWildcardEvaluators = wildcardEvaluators == parentWildcardEvaluators;

            // finalize evaluators list by including wildcard evaluators
            if (wildcardEvaluators != null) {
                if (CollectionUtils.isEmpty(this.evaluators)) {
                    this.evaluators = wildcardEvaluators;
                } else {
                    for (U evaluator : wildcardEvaluators) {
                        addEvaluator(evaluator);
                    }
                }
            }

            if (comparator != null) {
                if (!isSharingParentWildcardEvaluators && CollectionUtils.isNotEmpty(wildcardEvaluators)) {
                    wildcardEvaluators.sort(comparator);
                }

                if (evaluators != wildcardEvaluators && CollectionUtils.isNotEmpty(evaluators)) {
                    evaluators.sort(comparator);
                }
            }
        }

        private void removeEvaluatorFromSubtree(T evaluator) {
            if (CollectionUtils.isNotEmpty(wildcardEvaluators)) {
                if (wildcardEvaluators.remove(evaluator)) {
                    if (CollectionUtils.isEmpty(wildcardEvaluators)) {
                        wildcardEvaluators = null;
                    }
                    for (Map.Entry<Character, TrieNode<U>> entry : children.entrySet()) {
                        entry.getValue().removeEvaluatorFromSubtree(evaluator);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(evaluators)) {
                if (evaluators.remove(evaluator)) {
                    if (CollectionUtils.isEmpty(evaluators)) {
                        evaluators = null;
                    }
                }
            }
        }

        public void toString(StringBuilder sb) {
            String nodeValue = this.str;

            sb.append("nodeValue=").append(nodeValue);
            sb.append("; isSetup=").append(isSetup);
            sb.append("; isSharingParentWildcardEvaluators=").append(isSharingParentWildcardEvaluators);
            sb.append("; childCount=").append(children == null ? 0 : children.size());
            sb.append("; evaluators=[ ");
            if (evaluators != null) {
                for (U evaluator : evaluators) {
                    sb.append(evaluator.getId()).append(" ");
                }
            }
            sb.append("]");

            sb.append("; wildcardEvaluators=[ ");
            if (wildcardEvaluators != null) {
                for (U evaluator : wildcardEvaluators) {
                    sb.append(evaluator.getId()).append(" ");
                }
            }
        }

        public void toString(String prefix, StringBuilder sb) {
            String nodeValue = prefix + (str != null ? str : "");

            sb.append(prefix);
            toString(sb);
            sb.append("]\n");

            if (children != null) {
                for (Map.Entry<Character, TrieNode<U>> entry : children.entrySet()) {
                    TrieNode<U> child = entry.getValue();

                    child.toString(nodeValue, sb);
                }
            }
        }

        public void clear() {
            if (children != null) {
                children.clear();
            }

            evaluators         = null;
            wildcardEvaluators = null;
        }
    }
}
