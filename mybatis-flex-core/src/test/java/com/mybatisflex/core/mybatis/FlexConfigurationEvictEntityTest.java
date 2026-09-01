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
package com.mybatisflex.core.mybatis;

import com.mybatisflex.coretest.Account;
import com.mybatisflex.coretest.Article;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.type.JdbcType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * 覆盖 {@link FlexConfiguration#evictEntity(Class)} 以及
 * {@link FlexConfiguration} 新增的三个静态工具方法
 * （getDynamicMappedStatementCacheKeys / getDynamicMappedStatementCacheSize /
 * clearDynamicMappedStatementCache）。
 *
 * <p>dynamicMappedStatementCache 是 static 字段且为 private，本测试通过
 * 公开的 size / clear / evictEntity 三个方法做黑盒验证，不做反射。
 */
public class FlexConfigurationEvictEntityTest {

    private FlexConfiguration configuration;

    @Before
    public void setUp() {
        configuration = new FlexConfiguration();
        // 每个用例开始前清掉 static 缓存，避免与其他测试互相污染
        FlexConfiguration.clearDynamicMappedStatementCache();
    }

    @After
    public void tearDown() {
        FlexConfiguration.clearDynamicMappedStatementCache();
    }

    // ==================== evictEntity：清 dynamicMappedStatementCache ====================

    @Test
    public void evictEntityNullIsNoOp() {
        // 塞入一个匹配 Account 的条目
        seedDynamicMsCache("com.example.mapper.AccountMapper.selectList:" + Account.class.getName());

        int before = FlexConfiguration.getDynamicMappedStatementCacheSize();
        configuration.evictEntity(null);
        int after = FlexConfiguration.getDynamicMappedStatementCacheSize();

        Assert.assertEquals("null 入参不应清理任何条目", before, after);
    }

    @Test
    public void evictEntityRemovesMatchingDynamicMs() {
        String accountFqn = Account.class.getName();
        seedDynamicMsCache("mapper1.selectList:" + accountFqn);
        seedDynamicMsCache("mapper2.selectOne:" + accountFqn);
        seedDynamicMsCache("mapper3.selectList:" + Article.class.getName()); // 不匹配

        Assert.assertEquals(3, FlexConfiguration.getDynamicMappedStatementCacheSize());

        configuration.evictEntity(Account.class);

        Assert.assertEquals("应只移除 Account 的两条,保留 Article 的一条",
            1, FlexConfiguration.getDynamicMappedStatementCacheSize());
        Assert.assertTrue("剩下的那条应是 Article 的",
            FlexConfiguration.getDynamicMappedStatementCacheKeys().iterator().next()
                .endsWith(":" + Article.class.getName()));
    }

    @Test
    public void evictEntityIsIdempotent() {
        seedDynamicMsCache("mapper.selectList:" + Account.class.getName());

        configuration.evictEntity(Account.class);
        int sizeAfterFirst = FlexConfiguration.getDynamicMappedStatementCacheSize();

        // 第二次调用不应报错，也不应改变状态
        configuration.evictEntity(Account.class);
        int sizeAfterSecond = FlexConfiguration.getDynamicMappedStatementCacheSize();

        Assert.assertEquals(sizeAfterFirst, sizeAfterSecond);
    }

    // ==================== evictEntity：清 Configuration.resultMaps ====================

    @Test
    public void evictEntityRemovesMatchingResultMaps() {
        String accountFqn = Account.class.getName();
        // 模拟 flex 写入的 ResultMap：key = entityFQN
        addFakeResultMap(accountFqn, Account.class);
        // 以及一种变体：entityFQN-something（flex 内部可能生成的变体 key）
        addFakeResultMap(accountFqn + "-inline", Account.class);
        // 不相关的实体
        addFakeResultMap(Article.class.getName(), Article.class);

        Assert.assertTrue("Account ResultMap 应存在", configuration.hasResultMap(accountFqn));
        Assert.assertTrue("Account 变体 ResultMap 应存在", configuration.hasResultMap(accountFqn + "-inline"));
        Assert.assertTrue("Article ResultMap 应存在", configuration.hasResultMap(Article.class.getName()));

        configuration.evictEntity(Account.class);

        Assert.assertFalse("Account ResultMap 应被清掉", configuration.hasResultMap(accountFqn));
        Assert.assertFalse("Account 变体 ResultMap 应被清掉", configuration.hasResultMap(accountFqn + "-inline"));
        Assert.assertTrue("Article ResultMap 应保留", configuration.hasResultMap(Article.class.getName()));
    }

    // ==================== 静态工具方法 ====================

    @Test
    public void getSizeAndClearWork() {
        Assert.assertEquals(0, FlexConfiguration.getDynamicMappedStatementCacheSize());

        seedDynamicMsCache("a.b.C:com.example.Foo");
        seedDynamicMsCache("x.y.Z:com.example.Bar");

        Assert.assertEquals(2, FlexConfiguration.getDynamicMappedStatementCacheSize());

        FlexConfiguration.clearDynamicMappedStatementCache();

        Assert.assertEquals(0, FlexConfiguration.getDynamicMappedStatementCacheSize());
    }

    @Test
    public void getKeysReturnsSnapshot() {
        seedDynamicMsCache("mapper1:com.example.Foo");

        java.util.Set<String> snapshot = FlexConfiguration.getDynamicMappedStatementCacheKeys();
        Assert.assertEquals(1, snapshot.size());

        // 修改快照不应影响真实缓存
        snapshot.clear();
        Assert.assertEquals(1, FlexConfiguration.getDynamicMappedStatementCacheSize());
    }

    // ==================== 辅助方法 ====================

    /**
     * 往 dynamicMappedStatementCache 里塞一个占位条目。
     * 由于该字段是 private static，这里通过"清 -> 建 -> 再清 + 比对 size"的黑盒方式验证，
     * 但构造测试数据时必须能写入。这里走反射仅做 seed。
     */
    private void seedDynamicMsCache(String key) {
        try {
            java.lang.reflect.Field field = FlexConfiguration.class.getDeclaredField("dynamicMappedStatementCache");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> cache = (java.util.Map<String, Object>) field.get(null);
            cache.put(key, new Object()); // value 不重要，key 是匹配的依据
        } catch (Exception e) {
            throw new RuntimeException("seed dynamicMappedStatementCache failed", e);
        }
    }

    /**
     * 往 Configuration.resultMaps 塞一个假 ResultMap，用于验证清理逻辑。
     */
    private void addFakeResultMap(String id, Class<?> type) {
        ResultMapping rm = new ResultMapping.Builder(configuration, "id", "id", Long.class)
            .jdbcType(JdbcType.BIGINT)
            .build();
        ResultMap resultMap = new ResultMap.Builder(configuration, id, type, Collections.singletonList(rm))
            .build();
        // Configuration 没有 public 的 addResultMap 方法，但 addMappedStatement 会间接加。
        // 这里直接反射塞 resultMaps，简单直接。
        try {
            java.lang.reflect.Field field = org.apache.ibatis.session.Configuration.class.getDeclaredField("resultMaps");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, ResultMap> map = (java.util.Map<String, ResultMap>) field.get(configuration);
            map.put(id, resultMap);
        } catch (Exception e) {
            throw new RuntimeException("seed resultMaps failed", e);
        }
    }
}
