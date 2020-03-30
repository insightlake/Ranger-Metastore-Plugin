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

package org.apache.ranger.plugin.resourcematcher;

import com.google.common.collect.Lists;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RangerPathResourceMatcherTest {

    Object[][] data = {
            // { resource, policy, optWildcard, recursive, result
            { "/app/hive/test.db",    "/",                 true, false, false, "user" },
            { "/app/hive/test.db",    "/",                 true, true,  true, "user" },
            { "/app/hive/test.db",    "/*",                true, false, true, "user" },
            { "/app/hbase/test.tbl",  "/*",                true, false, true, "user" },
            { "/app/hive/test.db",    "/app",              true, false, false, "user" },
            { "/app/hive/test.db",    "/app/",             true, false, false, "user" },
            { "/app/hive/test.db",    "/app/",             true, true,  true, "user" },
            { "/app/hive/test.db",    "/app/*",            true, false, true, "user" },
            { "/app/hbase/test.tbl",  "/app/*",            true, false, true, "user" },
            { "/app/hive/test.db",    "/app/hive/*",       true, false, true, "user" },
            { "/app/hbase/test.tbl",  "/app/hive/*",       true, false, false, "user" },
            { "/app/hive/test.db",    "/app/hive/test*",   true, false, true, "user" },
            { "/app/hbase/test.tbl",  "/app/hive/test*",   true, false, false, "user" },
            { "/app/hive/test.db",    "/app/hive/test.db", true, false, true, "user" },
            { "/app/hbase/test.tbl",  "/app/hive/test.db", true, false, false, "user" },
    };

    @Test
    public void testIsMatch() throws Exception {
        for (Object[] row : data) {
            String resource = (String)row[0];
            String policyValue = (String)row[1];
            boolean optWildcard = (boolean)row[2];
            boolean isRecursive = (boolean)row[3];
            boolean result = (boolean)row[4];
            String user = (String) row[5];

            Map<String, Object> evalContext = new HashMap<>();
            RangerAccessRequestUtil.setCurrentUserInContext(evalContext, user);

            MatcherWrapper matcher = new MatcherWrapper(policyValue, optWildcard, isRecursive);
            assertEquals(getMessage(row), result, matcher.isMatch(resource, evalContext));
        }
    }

    String getMessage(Object[] row) {
        return String.format("Resource=%s, Policy=%s, optWildcard=%s, recursive=%s, result=%s",
                (String)row[0], (String)row[1], (boolean)row[2], (boolean)row[3], (boolean)row[4]);
    }

    static class MatcherWrapper extends RangerPathResourceMatcher {
        MatcherWrapper(String policyValue, boolean optWildcard, boolean isRecursive) {
            super.optWildCard = optWildcard;

            RangerPolicy.RangerPolicyResource policyResource = new RangerPolicy.RangerPolicyResource();
            policyResource.setIsRecursive(isRecursive);
            policyResource.setValues(Lists.newArrayList(policyValue));
            setPolicyResource(policyResource);

            init();
        }
    }

}