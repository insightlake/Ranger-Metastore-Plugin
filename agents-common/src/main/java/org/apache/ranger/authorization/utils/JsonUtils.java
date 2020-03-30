/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.plugin.model.RangerValidityRecurrence;
import org.apache.ranger.plugin.model.RangerValiditySchedule;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    private static final Log LOG = LogFactory.getLog(JsonUtils.class);

    private static final HashMap<String, String> MAP_STRING_STRING = new HashMap<>();

    private static final Gson gson;

    static {
        gson = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
                .create();
    }

    public static String mapToJson(Map<?, ?> map) {
        String ret = null;
        if (MapUtils.isNotEmpty(map)) {
            try {
                ret = gson.toJson(map);
            } catch (Exception e) {
                LOG.error("Invalid input data: ", e);
            }
        }
        return ret;
    }

    public static String listToJson(List<?> list) {
        String ret = null;
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                ret = gson.toJson(list);
            } catch (Exception e) {
                LOG.error("Invalid input data: ", e);
            }
        }
        return ret;
    }

    public static String objectToJson(Object object) {
        String ret = null;

        if(object != null) {
            try {
                ret = gson.toJson(object);
            } catch(Exception excp) {
                LOG.warn("objectToJson() failed to convert object to Json", excp);
            }
        }

        return ret;
    }

    public static <T> T jsonToObject(String jsonStr, Class<T> clz) {
        T ret = null;

        if(StringUtils.isNotEmpty(jsonStr)) {
            try {
                ret = gson.fromJson(jsonStr, clz);
            } catch(Exception excp) {
                LOG.warn("jsonToObject() failed to convert json to object: " + jsonStr, excp);
            }
        }

        return ret;
    }

    public static Map<String, String> jsonToMapStringString(String jsonStr) {
        Map<String, String> ret = null;

        if(StringUtils.isNotEmpty(jsonStr)) {
            try {
                ret = gson.fromJson(jsonStr, MAP_STRING_STRING.getClass());
            } catch(Exception excp) {
                LOG.warn("jsonToObject() failed to convert json to object: " + jsonStr, excp);
            }
        }

        return ret;
    }

    public static List<RangerValiditySchedule> jsonToRangerValiditySchedule(String jsonStr) {
        try {
            Type listType = new TypeToken<List<RangerValiditySchedule>>() {
            }.getType();
            return gson.fromJson(jsonStr, listType);
        } catch (Exception e) {
            LOG.error("Cannot get List<RangerValiditySchedule> from " + jsonStr, e);
            return null;
        }
    }
    public static List<RangerValidityRecurrence> jsonToRangerValidityRecurringSchedule(String jsonStr) {
        try {
            Type listType = new TypeToken<List<RangerValidityRecurrence>>() {
            }.getType();
            return gson.fromJson(jsonStr, listType);
        } catch (Exception e) {
            LOG.error("Cannot get List<RangerValidityRecurrence> from " + jsonStr, e);
            return null;
        }
    }
}
