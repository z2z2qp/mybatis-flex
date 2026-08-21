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

import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.util.LambdaUtil;
import org.jspecify.annotations.Nullable;

/**
 * mybatis-flex 内部缓存的统一操作入口（Facade）。
 *
 * <p>flex 在运行期维护了两组解析缓存：
 * <ul>
 *     <li>{@link TableInfoFactory} —— 实体类 / 表名 / Mapper 与 {@link com.mybatisflex.core.table.TableInfo} 的映射；</li>
 *     <li>{@link LambdaUtil} —— Lambda getter 的字段名 / 归属类 / QueryColumn 解析结果。</li>
 * </ul>
 *
 * <p>正常运行时缓存应保持稳定，不应清理。以下场景例外：
 * <ul>
 *     <li>开发期与 JRebel / Spring Boot DevTools 等热加载工具配合，实体类被重新加载后，
 *         需要强制 flex 用最新的 Class 重新解析；</li>
 *     <li>单元测试之间希望隔离，避免上一个用例遗留的 TableInfo 影响下一个用例。</li>
 * </ul>
 *
 * <p>本类只处理 flex 自身的缓存，<b>不会</b>触碰 MyBatis {@code Configuration} 内的 MappedStatement，
 * 也不会重建业务侧的 Spring bean。完整的热加载还需要业务侧在合适时机自行重建这些资源。
 *
 * @author mybatis-flex
 * @since 1.11.9
 */
public class FlexCaches {

    private FlexCaches() {
    }

    /**
     * 清除指定实体类在 flex 内部的所有缓存：
     * 委托给 {@link TableInfoFactory#evict(Class)} 与 {@link LambdaUtil#clear()}。
     *
     * <p>注意：{@link LambdaUtil} 的键是 Lambda 合成类（例如 {@code Foo$$Lambda$123}），
     * 与实体类不是一一对应，因此这里采用"整表清空"策略而非按实体粒度清理。
     * 这是刻意的取舍 —— 精细化清理需要遍历每个 QueryColumn 反查其归属实体，得不偿失。
     *
     * @param entityClass 实体类，允许为 {@code null}（此时只清 Lambda 缓存）
     * @return 一份简短的清理报告，形如 {@code "TableInfo=3, Lambda=17"}
     */
    public static String evictEntity(@Nullable Class<?> entityClass) {
        int tableInfoRemoved = TableInfoFactory.evict(entityClass);
        int lambdaRemoved = LambdaUtil.clear();
        return "TableInfo=" + tableInfoRemoved + ", Lambda=" + lambdaRemoved;
    }

    /**
     * 清空 flex 内部的全部缓存：
     * 委托给 {@link TableInfoFactory#clear()} 与 {@link LambdaUtil#clear()}。
     *
     * @return 一份简短的清理报告，形如 {@code "TableInfo=8, Lambda=17"}
     */
    public static String clearAll() {
        int tableInfoRemoved = TableInfoFactory.clear();
        int lambdaRemoved = LambdaUtil.clear();
        return "TableInfo=" + tableInfoRemoved + ", Lambda=" + lambdaRemoved;
    }

}
