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
package com.mybatisflex.core.keygen;

public class KeyGenerators {

    private KeyGenerators() {
    }

    /**
     * uuid 主键生成器
     * {@link com.mybatisflex.core.keygen.impl.UUIDKeyGenerator}
     */
    public static final String uuid = "uuid";

    /**
     * flexId 主键生成器
     * {@link com.mybatisflex.core.keygen.impl.FlexIDKeyGenerator}
     */
    public static final String flexId = "flexId";

    /**
     * 雪花算法主键生成器
     * {@link com.mybatisflex.core.keygen.impl.SnowFlakeIDKeyGenerator}
     */
    public static final String snowFlakeId = "snowFlakeId";

    /**
     * ulid 主键生成器
     * {@link com.mybatisflex.core.keygen.impl.ULIDKeyGenerator}
     */
    public static final String ulid = "ulid";
}
