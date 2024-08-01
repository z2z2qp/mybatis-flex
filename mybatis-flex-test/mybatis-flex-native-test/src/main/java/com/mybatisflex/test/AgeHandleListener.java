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

package com.mybatisflex.test;

import com.mybatisflex.annotation.InsertListener;

/**
 * 年龄处理监听器
 *
 * @author snow
 * @since 2023/4/28
 */
public class AgeHandleListener implements InsertListener {

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof AgeAware) {
            AgeAware ageAware = (AgeAware) entity;
            int age = ageAware.getAge();
            if (age < 0) {
                ageAware.setAge(0);
            }
        }
    }

    @Override
    public int order() {
        return 10;
    }

}
