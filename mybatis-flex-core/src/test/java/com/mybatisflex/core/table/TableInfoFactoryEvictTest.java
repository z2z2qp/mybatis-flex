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
package com.mybatisflex.core.table;

import com.mybatisflex.coretest.Account;
import com.mybatisflex.coretest.Article;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 覆盖 {@link TableInfoFactory#evict(Class)} 与 {@link TableInfoFactory#clear()}。
 */
public class TableInfoFactoryEvictTest {

    /**
     * 前后各清一次，避免与其他测试互相污染。
     */
    @Before
    public void resetBefore() {
        TableInfoFactory.clear();
    }

    @After
    public void resetAfter() {
        TableInfoFactory.clear();
    }

    @Test
    public void evictNullReturnsZero() {
        Assert.assertEquals(0, TableInfoFactory.evict(null));
    }

    @Test
    public void evictRemovesEntityAndReturnsFreshInstanceOnNextFetch() {
        // 先触发缓存
        TableInfo before = TableInfoFactory.ofEntityClass(Account.class);
        Assert.assertNotNull(before);

        // 清除
        int removed = TableInfoFactory.evict(Account.class);
        Assert.assertTrue("应至少清掉 entityTableMap 中的一条", removed >= 1);

        // 第二次 evict 应返回 0（幂等）
        Assert.assertEquals(0, TableInfoFactory.evict(Account.class));

        // 再取一次，flex 会重新构建 —— 应当是新的 TableInfo 实例
        TableInfo after = TableInfoFactory.ofEntityClass(Account.class);
        Assert.assertNotNull(after);
        Assert.assertNotSame("evict 后应重新构建 TableInfo", before, after);
    }

    @Test
    public void clearEmptiesAllCaches() {
        // 预热两张表
        TableInfoFactory.ofEntityClass(Account.class);
        TableInfoFactory.ofEntityClass(Article.class);

        int removed = TableInfoFactory.clear();
        Assert.assertTrue("clear() 应清掉至少两条 entityTableMap 记录", removed >= 2);

        // clear() 后紧接着再调用一次，返回 0
        Assert.assertEquals(0, TableInfoFactory.clear());

        // 后续依旧能取到 TableInfo
        Assert.assertNotNull(TableInfoFactory.ofEntityClass(Account.class));
    }

}
