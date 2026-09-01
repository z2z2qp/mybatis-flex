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

import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.coretest.Account;
import com.mybatisflex.coretest.Article;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;

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

    // ==================== SqlSessionFactory 重载（MyBatis 层清理）====================

    @Test
    public void evictEntityWithNullSessionFactoryDegrades() {
        // SqlSessionFactory 为 null 时，行为等同于无 SqlSessionFactory 的重载
        TableInfoFactory.ofEntityClass(Account.class);
        String report = FlexCaches.evictEntity((SqlSessionFactory) null, Account.class);
        Assert.assertTrue(report.contains("TableInfo="));
        Assert.assertTrue(report.contains("Lambda="));
        Assert.assertFalse("不应包含 MappedStatement 段", report.contains("MappedStatement="));
        Assert.assertFalse("不应包含 ResultMap 段", report.contains("ResultMap="));
    }

    @Test
    public void evictEntityWithNonFlexConfigurationDegrades() {
        // Configuration 不是 FlexConfiguration 时，降级到只清 flex 自身
        SqlSessionFactory ssf = mockSqlSessionFactory(new Configuration());
        TableInfoFactory.ofEntityClass(Account.class);
        String report = FlexCaches.evictEntity(ssf, Account.class);
        Assert.assertTrue(report.contains("TableInfo="));
        Assert.assertFalse("非 FlexConfiguration 不应包含 MappedStatement 段", report.contains("MappedStatement="));
    }

    @Test
    public void evictEntityWithFlexConfigurationClearsMyBatisCaches() {
        FlexConfiguration flexCfg = new FlexConfiguration();
        SqlSessionFactory ssf = mockSqlSessionFactory(flexCfg);

        // 预热 TableInfo
        TableInfoFactory.ofEntityClass(Account.class);

        // 造 dynamicMappedStatementCache 条目
        seedDynamicMsCache("mapper1.selectList:" + Account.class.getName());
        seedDynamicMsCache("mapper2.selectOne:" + Account.class.getName());
        seedDynamicMsCache("mapper3.selectList:" + Article.class.getName()); // 不匹配

        // 造 resultMaps 条目
        addFakeResultMap(flexCfg, Account.class.getName(), Account.class);
        addFakeResultMap(flexCfg, Account.class.getName() + "-inline", Account.class);
        addFakeResultMap(flexCfg, Article.class.getName(), Article.class);

        String report = FlexCaches.evictEntity(ssf, Account.class);

        Assert.assertTrue("报告应含 TableInfo 段", report.contains("TableInfo="));
        Assert.assertTrue("报告应含 Lambda 段", report.contains("Lambda="));
        Assert.assertTrue("报告应含 MappedStatement 段", report.contains("MappedStatement="));
        Assert.assertTrue("报告应含 ResultMap 段", report.contains("ResultMap="));

        // 验证 dynamicMappedStatementCache：Account 的两条被清，Article 的一条保留
        Assert.assertEquals(1, FlexConfiguration.getDynamicMappedStatementCacheSize());

        // 验证 resultMaps：Account 的两个被清，Article 的保留
        Assert.assertFalse(flexCfg.hasResultMap(Account.class.getName()));
        Assert.assertFalse(flexCfg.hasResultMap(Account.class.getName() + "-inline"));
        Assert.assertTrue(flexCfg.hasResultMap(Article.class.getName()));
    }

    @Test
    public void clearAllWithNullSessionFactoryDegrades() {
        TableInfoFactory.ofEntityClass(Account.class);
        String report = FlexCaches.clearAll((SqlSessionFactory) null);
        Assert.assertTrue(report.contains("TableInfo="));
        Assert.assertFalse("null ssf 不应含 MappedStatement 段", report.contains("MappedStatement="));
    }

    @Test
    public void clearAllWithFlexConfigurationClearsDynamicMs() {
        FlexConfiguration flexCfg = new FlexConfiguration();
        SqlSessionFactory ssf = mockSqlSessionFactory(flexCfg);

        TableInfoFactory.ofEntityClass(Account.class);
        seedDynamicMsCache("a.b.C:" + Account.class.getName());
        seedDynamicMsCache("x.y.Z:" + Article.class.getName());

        String report = FlexCaches.clearAll(ssf);

        Assert.assertTrue("报告应含 MappedStatement 段", report.contains("MappedStatement="));
        Assert.assertEquals("dynamicMappedStatementCache 应被全清",
            0, FlexConfiguration.getDynamicMappedStatementCacheSize());

        // clearAll 不清 resultMaps 全量，验证 Article 的还在
        addFakeResultMap(flexCfg, Article.class.getName(), Article.class);
        FlexCaches.clearAll(ssf);
        Assert.assertTrue("clearAll 不应动 resultMaps 全量",
            flexCfg.hasResultMap(Article.class.getName()));
    }

    // ==================== 辅助方法 ====================

    private SqlSessionFactory mockSqlSessionFactory(Configuration cfg) {
        // 用动态代理避免 SqlSessionFactory 接口里 openSession 重载方法的签名冲突
        // （TransactionIsolationLevel 在 java.sql 和 org.apache.ibatis.session 都有，
        //  且参数数量相同，匿名类实现时 erasure 后会冲突）
        return (SqlSessionFactory) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{SqlSessionFactory.class},
            (proxy, method, args) -> {
                if ("getConfiguration".equals(method.getName())) {
                    return cfg;
                }
                return null;
            });
    }

    private void seedDynamicMsCache(String key) {
        try {
            Field field = FlexConfiguration.class.getDeclaredField("dynamicMappedStatementCache");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> cache = (java.util.Map<String, Object>) field.get(null);
            cache.put(key, new Object());
        } catch (Exception e) {
            throw new RuntimeException("seed dynamicMappedStatementCache failed", e);
        }
    }

    private void addFakeResultMap(Configuration cfg, String id, Class<?> type) {
        ResultMapping rm = new ResultMapping.Builder(cfg, "id", "id", Long.class)
            .jdbcType(JdbcType.BIGINT)
            .build();
        ResultMap resultMap = new ResultMap.Builder(cfg, id, type, Collections.singletonList(rm))
            .build();
        try {
            Field field = Configuration.class.getDeclaredField("resultMaps");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, ResultMap> map = (java.util.Map<String, ResultMap>) field.get(cfg);
            map.put(id, resultMap);
        } catch (Exception e) {
            throw new RuntimeException("seed resultMaps failed", e);
        }
    }

}
