/**
 * Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mybatisflex.core.util;

import java.util.*;
import java.util.function.Function;


public class CollectionUtil {


    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }


    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }


    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }


    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 合并 list
     */
    public static <T> List<T> merge(List<T> list, List<T> other) {
        if (list == null && other == null) {
            return new ArrayList<>();
        } else if (isEmpty(other) && list != null) {
            return list;
        } else if (isEmpty(list)) {
            return other;
        }
        var newList = new ArrayList<T>(list);
        newList.addAll(other);
        return newList;
    }


    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * 主要是用于修复 concurrentHashMap 在 jdk1.8 下的死循环问题
     *
     * @see <a href="https://bugs.openjdk.org/browse/JDK-8161372">https://bugs.openjdk.org/browse/JDK-8161372</a>
     */
    public static <K, V> V computeIfAbsent(Map<K, V> concurrentHashMap, K key, Function<? super K, ? extends V> mappingFunction) {
        var v = concurrentHashMap.get(key);
        if (v != null) {
            return v;
        }
        return concurrentHashMap.computeIfAbsent(key, mappingFunction);
    }

    public static <T> Set<T> newHashSet(T... elements) {
        return new HashSet<>(Set.of(elements));
    }

    public static <T> List<T> newArrayList(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    public static <T> List<T> toList(Collection<T> collection) {
        if (collection instanceof List) {
            return (List<T>) collection;
        } else {
            return new ArrayList<>(collection);
        }
    }

}
