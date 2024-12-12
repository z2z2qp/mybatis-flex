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

package com.mybatisflex.solon.integration;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.mybatis.FlexSqlSessionFactoryBuilder;
import com.mybatisflex.core.row.RowMapperInvoker;
import com.mybatisflex.solon.mybtais.MybatisAdapterDefault;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.noear.solon.Utils;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.core.Props;
import org.noear.solon.core.VarHolder;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.util.ClassUtil;

import javax.sql.DataSource;

/**
 * MyBatis-Flex 适配器。
 *
 * @author noear
 * @since 2.2
 */
public class MybatisAdapterFlex extends MybatisAdapterDefault {
    private FlexSqlSessionFactoryBuilder factoryBuilderPlus;
    private FlexGlobalConfig globalConfig;
    private RowMapperInvoker rowMapperInvoker;
    private Class<?> typeAliasesBaseType;

    protected MybatisAdapterFlex(BeanWrap dsWrap) {
        super(dsWrap);

        factoryBuilderPlus = new FlexSqlSessionFactoryBuilder();
        initAfter(dsWrap);
    }

    protected MybatisAdapterFlex(BeanWrap dsWrap, Props dsProps) {
        super(dsWrap, dsProps);

        factoryBuilderPlus = new FlexSqlSessionFactoryBuilder();
        initAfter(dsWrap);
    }

    protected void initAfter(BeanWrap dsWrap) {
        globalConfig.setSqlSessionFactory(getFactory());
    }

    @Override
    protected DataSource getDataSource() {
        return new FlexRoutingDataSource(dsWrap.name(), dsWrap.raw());
    }

    @Override
    protected void initConfiguration(Environment environment) {
        //for configuration section
        config = new FlexConfiguration(environment);

        String typeAliasesBaseTypeStr = dsProps.get("typeAliasesSuperType");
        if (Utils.isNotEmpty(typeAliasesBaseTypeStr)) {
            typeAliasesBaseType = ClassUtil.loadClass(typeAliasesBaseTypeStr);
        }

        Props cfgProps = dsProps.getProp("configuration");
        if (cfgProps.size() > 0) {
            Utils.injectProperties(config, cfgProps);
        }


        //for globalConfig section
        if (dsWrap.typed()) {
            globalConfig = FlexGlobalConfig.getDefaultConfig();
        } else {
            globalConfig = new FlexGlobalConfig();
        }

        if (globalConfig.getKeyConfig() == null) {
            globalConfig.setKeyConfig(new FlexGlobalConfig.KeyConfig());
        }

        Props globalProps = dsProps.getProp("globalConfig");
        if (globalProps.size() > 0) {
            //尝试配置注入
            Utils.injectProperties(globalConfig, globalProps);
        }
        globalConfig.setConfiguration(config);

        FlexGlobalConfig.setConfig(environment.getId(), globalConfig, false);

        //增加事件扩展机制
        EventBus.publish(globalConfig);
    }

    /**
     * 获取全局配置
     */
    public FlexGlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    @Override
    public SqlSessionFactory getFactory() {
        if (factory == null) {
            factory = factoryBuilderPlus.build(getConfiguration());
        }

        return factory;
    }

    @Override
    public void injectTo(VarHolder varH) {
        super.injectTo(varH);

        // @Db("db1") FlexGlobalConfig globalConfig
        if (FlexGlobalConfig.class.isAssignableFrom(varH.getType())) {
            varH.setValue(this.getGlobalConfig());
            return;
        }

        // @Db("db1") RowMapperInvoker rowMapper
        if (RowMapperInvoker.class.equals(varH.getType())) {
            if (rowMapperInvoker == null) {
                rowMapperInvoker = new RowMapperInvoker(getFactory());
            }
            varH.setValue(rowMapperInvoker);
        }
    }

    @Override
    protected boolean isTypeAliasesType(Class<?> type) {
        //typeAliasesSuperType
        if (typeAliasesBaseType == null) {
            return true;
        } else {
            return typeAliasesBaseType.isAssignableFrom(type);
        }
    }

    @Override
    protected boolean isTypeAliasesKey(String key) {
        return super.isTypeAliasesKey(key) || key.startsWith("typeAliasesPackage[");
    }

    @Override
    protected boolean isTypeHandlersKey(String key) {
        return super.isTypeHandlersKey(key) || key.startsWith("typeHandlersPackage[");
    }

    @Override
    protected boolean isMappersKey(String key) {
        return super.isMappersKey(key) || key.startsWith("mapperLocations[");
    }
}
