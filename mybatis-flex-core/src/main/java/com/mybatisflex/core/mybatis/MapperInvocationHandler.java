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
package com.mybatisflex.core.mybatis;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.dialect.DialectFactory;
import com.mybatisflex.core.row.RowMapper;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.util.StringUtil;
import org.apache.ibatis.reflection.ExceptionUtil;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author michael
 * @author norkts
 */
public class MapperInvocationHandler implements InvocationHandler {

    private final Object mapper;
    private final FlexDataSource dataSource;

    public MapperInvocationHandler(Object mapper, DataSource dataSource) {
        this.mapper = mapper;
        if (dataSource instanceof FlexDataSource) {
            this.dataSource = (FlexDataSource) dataSource;
        } else {
            this.dataSource = null;
        }
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean needClearDsKey = false;
        boolean needClearDbType = false;
        try {
            //获取用户动态指定，由用户指定数据源，则应该有用户清除
            String dataSourceKey = DataSourceKey.get();
            if (StringUtil.isBlank(dataSourceKey)) {
                //通过 @UseDataSource 或者 @Table(dataSource) 去获取
                String configDataSourceKey = getConfigDataSourceKey(method, proxy);
                if (StringUtil.isNotBlank(configDataSourceKey)) {
                    dataSourceKey = configDataSourceKey;
                    DataSourceKey.use(dataSourceKey);
                    needClearDsKey = true;
                }
            }

            //最终通过数据源 自定义分片 策略去获取
            String shardingDataSourceKey = DataSourceKey.getByShardingStrategy(dataSourceKey, proxy, method, args);
            if (shardingDataSourceKey != null && !shardingDataSourceKey.equals(dataSourceKey)) {
                DataSourceKey.use(shardingDataSourceKey);
                needClearDsKey = true;
            }

            //优先获取用户自己配置的 dbType
            DbType dbType = DialectFactory.getHintDbType();

            if (dbType == null) {
                if (shardingDataSourceKey != null && dataSource != null) {
                    //使用最终分片获取数据源类型
                    dbType = dataSource.getDbType(shardingDataSourceKey);
                }

                if (dbType == null && dataSourceKey != null && dataSource != null) {
                    dbType = dataSource.getDbType(dataSourceKey);
                }

                //设置了dbTypeGlobal，那么就使用全局的dbTypeGlobal
                if (dbType == null) {
                    dbType = DialectFactory.getGlobalDbType();
                }

                if (dbType == null) {
                    dbType = FlexGlobalConfig.getDefaultConfig().getDbType();
                }

                DialectFactory.setHintDbType(dbType);
                needClearDbType = true;
            }
            return method.invoke(mapper, args);
        } catch (Throwable e) {
            throw ExceptionUtil.unwrapThrowable(e);
        } finally {
            if (needClearDbType) {
                DialectFactory.clearHintDbType();
            }
            if (needClearDsKey) {
                DataSourceKey.clear();
            }
        }
    }


    private static String getConfigDataSourceKey(Method method, Object proxy) {
        UseDataSource useDataSource = method.getAnnotation(UseDataSource.class);
        if (useDataSource != null && StringUtil.isNotBlank(useDataSource.value())) {
            return useDataSource.value();
        }

        Class<?>[] interfaces = proxy.getClass().getInterfaces();
        for (Class<?> anInterface : interfaces) {
            UseDataSource annotation = anInterface.getAnnotation(UseDataSource.class);
            if (annotation != null) {
                return annotation.value();
            }
        }

        if (interfaces[0] != RowMapper.class) {
            TableInfo tableInfo = TableInfoFactory.ofMapperClass(interfaces[0]);
            if (tableInfo != null) {
                String dataSourceKey = tableInfo.getDataSource();
                if (StringUtil.isNotBlank(dataSourceKey)) {
                    return dataSourceKey;
                }
            }
        }
        return null;
    }


}
