/*
 *  Copyright (c) 2022-2025, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mybatisflex.core.mybatis.executor;

import com.mybatisflex.core.FlexConsts;
import org.apache.ibatis.cache.CacheKey;

import java.util.Arrays;
import java.util.Map;

public interface CacheKeyBuilder {

    default CacheKey buildCacheKey(CacheKey cacheKey, Object parameterObject){
        if (parameterObject instanceof Map map && map.containsKey(FlexConsts.SQL_ARGS)){
            cacheKey.update(Arrays.toString((Object[]) map.get(FlexConsts.SQL_ARGS)));
        }
        return cacheKey;
    }

}
