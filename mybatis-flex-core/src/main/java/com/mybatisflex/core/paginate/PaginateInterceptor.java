/*
 *  Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
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
package com.mybatisflex.core.paginate;

import com.mybatisflex.core.dialect.LimitOffsetProcessor;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 分页拦截器。
 *
 * @author 王帅
 * @since 2023-05-18
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
@SuppressWarnings({"rawtypes", "unchecked"})
public class PaginateInterceptor implements Interceptor {

    /**
     * 拦截 Executor 的两个 query 方法，实现分页操作。<br>
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler)} <br>
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取 Executor 对象 query 方法的参数列表
        Object[] args = invocation.getArgs();
        // 获取两个 query 方法都有的参数
        MappedStatement ms = (MappedStatement) args[0];
        Object parameters = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler resultHandler = (ResultHandler) args[3];
        // 获取 Executor 对象
        Executor executor = (Executor) invocation.getTarget();

        BoundSql boundSql;

        // 两个 query 方法参数不相同，获取 BoundSql 的方式不同
        if (args.length == 4) {
            // 4 个参数时
            boundSql = ms.getBoundSql(parameters);
        } else {
            // 6 个参数时
            boundSql = (BoundSql) args[5];
        }

        // 获取拦截查询方法是否有 Page 参数
        Page page = getPage(parameters);
        // 没有 Page 参数就认为不是分页操作
        if (page == null) {
            return invocation.proceed();
        }

        // 设置分页数据
        page.setRecords(executeQuery(executor, ms, parameters, rowBounds, resultHandler, boundSql, page));
        // 设置数据总数量
        page.setTotalRow(executeCount(executor, ms, parameters, rowBounds, resultHandler, boundSql));

        return Collections.singletonList(page);
    }

    /**
     * 查询被拦截方法的参数有没有 {@link Page} 对象。
     */
    private Page getPage(Object parameters) {
        if (parameters != null) {
            if (parameters instanceof Map) {
                Map<?, ?> parameterMap = (Map<?, ?>) parameters;
                for (Entry entry : parameterMap.entrySet()) {
                    if (entry.getValue() instanceof Page) {
                        return (Page) entry.getValue();
                    }
                }
            } else if (parameters instanceof Page) {
                return (Page) parameters;
            }
        }
        return null;
    }

    /**
     * 根据 {@link Page} 对象中页码（{@link Page#getPageNumber()}）
     * 和每页显示的数量（{@link Page#getPageSize()}），构建 SQL 语句，
     * 查询指定页的内容。
     */
    private List executeQuery(Executor executor, MappedStatement ms,
                              Object parameters, RowBounds rowBounds,
                              ResultHandler resultHandler, BoundSql boundSql,
                              Page page) throws SQLException {
        // 获取分页 SQL 语句
        String pageSqlStr = getPageSqlStr(boundSql.getSql(), page);
        // 构建分页查询 BoundSql 对象
        BoundSql pageSql = new BoundSql(ms.getConfiguration(), pageSqlStr, boundSql.getParameterMappings(), parameters);
        // 将原 SQL 中的占位符对应的参数，添加到新分页 SQL 中
        boundSql.getAdditionalParameters().forEach(pageSql::setAdditionalParameter);
        // 获取 CacheKey 对象
        CacheKey cacheKey = executor.createCacheKey(ms, parameters, rowBounds, pageSql);
        // 执行分页 SQL 查询数据
        return executor.query(ms, parameters, rowBounds, resultHandler, cacheKey, pageSql);
    }

    /**
     * 构建分页 SQL 语句。
     */
    private String getPageSqlStr(String originalSql, Page page) {
        int size = page.getPageSize();
        int number = page.getPageNumber();
        int limitRows = (number - 1) * size;
        return LimitOffsetProcessor.MYSQL.process(new StringBuilder(originalSql), QueryWrapper.create(), size, limitRows).toString();
    }

    /**
     * 查询符合原 SQL 条件的数据数量。
     */
    private Long executeCount(Executor executor, MappedStatement ms,
                              Object parameters, RowBounds rowBounds,
                              ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 获取 count 查询的 MappedStatement 对象
        MappedStatement countMs = getCountMappedStatement(ms);

        BoundSql countSql;

        if (countMs != null) {
            countSql = countMs.getBoundSql(parameters);
        } else {
            // 获取不到则构建新的 MappedStatement 对象
            countMs = buildCountMappedStatement(ms);
            // 获取 count 查询 SQL
            String countSqlStr = getCountSqlStr(boundSql.getSql());
            // 构建 count 查询 BoundSql 对象
            countSql = new BoundSql(ms.getConfiguration(), countSqlStr, boundSql.getParameterMappings(), parameters);
            // 将原 SQL 中的占位符对应的参数，添加到新 count 查询 SQL 中
            boundSql.getAdditionalParameters().forEach(countSql::setAdditionalParameter);
        }

        // 获取 CacheKey 对象
        CacheKey countKey = executor.createCacheKey(countMs, parameters, rowBounds, countSql);
        // 执行 count 查询 SQL 查询数量
        Object result = executor.query(countMs, parameters, rowBounds, resultHandler, countKey, countSql);
        // 处理结果，返回总数量
        return ((Number) ((List) result).get(0)).longValue();
    }

    /**
     * 尝试获取已经存在的 {@link MappedStatement} 对象。
     */
    private MappedStatement getCountMappedStatement(MappedStatement ms) {
        String msId = ms.getId().concat("_COUNT");
        MappedStatement mappedStatement = null;
        try {
            mappedStatement = ms.getConfiguration().getMappedStatement(msId, false);
        } catch (Exception ignored) {
            // do nothing.
        }
        return mappedStatement;
    }

    /**
     * 构建 count 查询的 {@link MappedStatement} 对象。
     */
    private MappedStatement buildCountMappedStatement(MappedStatement ms) {
        String countId = ms.getId() + "_Count";
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), countId, ms.getSqlSource(), ms.getSqlCommandType());
        return builder.resource(ms.getResource())
                .fetchSize(ms.getFetchSize())
                .statementType(ms.getStatementType())
                .keyGenerator(ms.getKeyGenerator())
                .timeout(ms.getTimeout())
                .parameterMap(ms.getParameterMap())
                .resultMaps(Collections.singletonList(new ResultMap.Builder(ms.getConfiguration(), ms.getId(), Long.class, Collections.emptyList()).build()))
                .resultSetType(ms.getResultSetType())
                .cache(ms.getCache())
                .flushCacheRequired(ms.isFlushCacheRequired())
                .useCache(ms.isUseCache())
                .build();
    }

    /**
     * 构建 count 查询 SQL 语句。
     */
    private String getCountSqlStr(String originalSql) {
        return String.format("SELECT COUNT(*) FROM (%s) TOTAL", originalSql);
    }

}