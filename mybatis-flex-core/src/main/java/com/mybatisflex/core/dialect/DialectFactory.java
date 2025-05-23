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
package com.mybatisflex.core.dialect;


import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.dialect.impl.*;
import com.mybatisflex.core.util.MapUtil;
import com.mybatisflex.core.util.ObjectUtil;

import java.util.EnumMap;
import java.util.Map;

/**
 * 方言工厂类，用于创建方言
 */
public class DialectFactory {

    private DialectFactory() {
    }

    /**
     * 数据库类型和方言的映射关系，可以通过其读取指定的方言，亦可能通过其扩展其他方言
     * 比如，在 mybatis-flex 实现的方言中有 bug 或者 有自己的独立实现，可以添加自己的方言实现到
     * 此 map 中，用于覆盖系统的方言实现
     */
    private static final Map<DbType, IDialect> dialectMap = new EnumMap<>(DbType.class);
    /**
     * 通过设置当前线程的数据库类型，以达到在代码执行时随时切换方言的功能
     */
    private static final ThreadLocal<DbType> dbTypeThreadLocal = new ThreadLocal<>();

    /**
     * 获取方言
     *
     * @return IDialect
     */
    public static IDialect getDialect() {
        DbType dbType = ObjectUtil.requireNonNullElse(dbTypeThreadLocal.get(),
            FlexGlobalConfig.getDefaultConfig().getDbType());
        return MapUtil.computeIfAbsent(dialectMap, dbType, DialectFactory::createDialect);
    }

    /**
     * 设置当前线程的 dbType
     *
     * @param dbType
     */
    public static void setHintDbType(DbType dbType) {
        dbTypeThreadLocal.set(dbType);
    }

    /**
     * 获取当前线程的 dbType
     *
     * @return dbType
     */
    public static DbType getHintDbType() {
        return dbTypeThreadLocal.get();
    }


    /**
     * 清除当前线程的 dbType
     */
    public static void clearHintDbType() {
        dbTypeThreadLocal.remove();
    }


    /**
     * 可以为某个 dbType 注册（新增或覆盖）自己的方言
     *
     * @param dbType  数据库类型
     * @param dialect 方言的实现
     */
    public static void registerDialect(DbType dbType, IDialect dialect) {
        dialectMap.put(dbType, dialect);
    }


    private static IDialect createDialect(DbType dbType) {
        return switch (dbType) {
            case MYSQL, H2, MARIADB, GBASE, OSCAR, XUGU, OCEAN_BASE, CUBRID, GOLDILOCKS, CSIIDB, HIVE, DORIS, GOLDENDB, SUNDB, YASDB ->
                new CommonsDialectImpl(KeywordWrap.BACK_QUOTE, LimitOffsetProcessor.MYSQL);
            case CLICK_HOUSE -> new ClickhouseDialectImpl(KeywordWrap.NONE, LimitOffsetProcessor.MYSQL);
            case GBASE_8S -> new CommonsDialectImpl(KeywordWrap.NONE, LimitOffsetProcessor.MYSQL);
            case DM -> new DmDialect();
            case ORACLE -> new OracleDialect();
            case GAUSS -> new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, LimitOffsetProcessor.ORACLE);
            case POSTGRE_SQL, SQLITE, HSQL, KINGBASE_ES, PHOENIX, SAP_HANA, IMPALA, HIGH_GO, VERTICA, REDSHIFT,
                    OPENGAUSS, UXDB, LEALONE, DUCKDB, GBASE_8C, GBASE_8S_PG, VASTBASE, TRINO, PRESTO ->
                new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, LimitOffsetProcessor.POSTGRESQL);
            case TDENGINE -> new CommonsDialectImpl(KeywordWrap.BACK_QUOTE, LimitOffsetProcessor.POSTGRESQL);
            case ORACLE_12C -> new OracleDialect(LimitOffsetProcessor.DERBY);
            case FIREBIRD, DB2 -> new CommonsDialectImpl(KeywordWrap.NONE, LimitOffsetProcessor.DERBY);
            case DB2_1005 -> new DB2105Dialect(KeywordWrap.NONE, DB2105Dialect.DB2105LimitOffsetProcessor.DB2105);
            case SQLSERVER -> new SqlserverDialectImpl(KeywordWrap.SQUARE_BRACKETS, LimitOffsetProcessor.SQLSERVER);
            case SQLSERVER_2005 ->
                new Sqlserver2005DialectImpl(KeywordWrap.SQUARE_BRACKETS, LimitOffsetProcessor.SQLSERVER_2005);
            case INFORMIX -> new CommonsDialectImpl(KeywordWrap.NONE, LimitOffsetProcessor.INFORMIX);
            case SINODB -> new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, LimitOffsetProcessor.SINODB);
            case SYBASE -> new CommonsDialectImpl(KeywordWrap.DOUBLE_QUOTATION, LimitOffsetProcessor.SYBASE);
            default -> new CommonsDialectImpl();
        };
    }

}
