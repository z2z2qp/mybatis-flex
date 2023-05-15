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
package com.mybatisflex.annotation;

/**
 * 监听设置值
 * @param <T> 监听的对象
 */
public interface SetListener<T> extends Listener {

    /**
     * 设置entity的值，已return值为准
     * @param entity 返回的对象
     * @param property 对象属性key
     * @param value 数据库获取的值
     * @return 数据值，会set到entity
     */
    Object onSet(T entity, String property, Object value);
}
