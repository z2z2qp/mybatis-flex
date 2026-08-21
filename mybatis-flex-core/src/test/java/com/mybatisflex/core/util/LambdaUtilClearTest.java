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
package com.mybatisflex.core.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 覆盖 {@link LambdaUtil#clear()}。
 */
public class LambdaUtilClearTest {

    /**
     * 每个用例前后都清空缓存，避免与 flex 其它 Lambda 相关测试（例如 {@code LambdaUtilTest.testIssue516}
     * 的绝对 size 断言）互相污染 —— 每一处 {@code TestAccount::getName} call-site
     * 都会生成一份独立的 synthetic Lambda 类，作为独立 key 进入 {@code fieldNameMap}。
     */
    @Before
    public void resetBefore() {
        LambdaUtil.clear();
    }

    @After
    public void resetAfter() {
        LambdaUtil.clear();
    }

    @Test
    public void clearEmptiesFieldNameCache() {
        // 触发缓存：同一 call-site 多次调用只占一格
        for (int i = 0; i < 10; i++) {
            LambdaUtil.getFieldName(TestAccount::getName);
            LambdaUtil.getFieldName(TestAccount::getAge);
        }
        Assert.assertTrue("清理前应至少有 1 条 fieldNameMap 记录",
            LambdaUtil.getFieldNameMap().size() >= 1);

        int removed = LambdaUtil.clear();
        Assert.assertTrue("clear() 至少应清掉刚才缓存的 fieldNameMap 条目", removed >= 1);
        Assert.assertEquals(0, LambdaUtil.getFieldNameMap().size());

        // clear() 幂等：紧接再调，返回 0
        Assert.assertEquals(0, LambdaUtil.clear());

        // 清理后 getFieldName 仍能正常工作（会重新解析并入缓存）
        Assert.assertEquals("name", LambdaUtil.getFieldName(TestAccount::getName));
        Assert.assertEquals(1, LambdaUtil.getFieldNameMap().size());
    }

}
