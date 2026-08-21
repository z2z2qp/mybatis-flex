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
package com.mybatisflex.core;

import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.coretest.Account;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 覆盖 {@link FlexCaches} 门面。
 */
public class FlexCachesTest {

    /**
     * 前后各清一次缓存，避免与其他测试（例如 {@code LambdaUtilTest.testIssue516} 的绝对 size 断言）互相污染。
     */
    @Before
    public void resetBefore() {
        FlexCaches.clearAll();
    }

    @After
    public void resetAfter() {
        FlexCaches.clearAll();
    }

    @Test
    public void evictEntityReportsCounts() {
        TableInfoFactory.ofEntityClass(Account.class);
        String report = FlexCaches.evictEntity(Account.class);
        Assert.assertNotNull(report);
        Assert.assertTrue("report 应包含 TableInfo 段", report.contains("TableInfo="));
        Assert.assertTrue("report 应包含 Lambda 段", report.contains("Lambda="));
    }

    @Test
    public void clearAllEmptiesEverything() {
        TableInfo ti = TableInfoFactory.ofEntityClass(Account.class);
        Assert.assertNotNull(ti);

        String report = FlexCaches.clearAll();
        Assert.assertNotNull(report);

        // 紧接一次 clear，两个计数应为 0
        Assert.assertEquals("TableInfo=0, Lambda=0", FlexCaches.clearAll());
    }

    @Test
    public void evictEntityAcceptsNull() {
        // 仅 Lambda 会被清（可能为 0），不应抛异常
        String report = FlexCaches.evictEntity(null);
        Assert.assertTrue(report.startsWith("TableInfo=0"));
    }

}
