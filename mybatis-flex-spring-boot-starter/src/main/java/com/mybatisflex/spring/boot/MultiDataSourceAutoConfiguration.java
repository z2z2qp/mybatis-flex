/*
 *  Copyright (c) 2022-2024, Mybatis-Flex (fuhai999@gmail.com).
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
package com.mybatisflex.spring.boot;

import com.mybatisflex.core.datasource.DataSourceBuilder;
import com.mybatisflex.core.datasource.DataSourceDecipher;
import com.mybatisflex.core.datasource.DataSourceManager;
import com.mybatisflex.core.datasource.FlexDataSource;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.util.MapUtil;
import com.mybatisflex.spring.boot.MybatisFlexProperties.SeataConfig;
import com.mybatisflex.spring.datasource.DataSourceAdvice;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import javax.sql.DataSource;
import java.util.Map;

/**
 * MyBatis-Flex 多数据源的配置支持。
 *
 * @author michael
 * @author 王帅
 */
@ConditionalOnMybatisFlexDatasource()
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MybatisFlexProperties.class)
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@AutoConfigureBefore(value = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class}
    , name = {"com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure",
    "com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure"})
public class MultiDataSourceAutoConfiguration {

    private final String master;

    private final Map<String, Map<String, String>> dataSourceProperties;

    private final SeataConfig seataConfig;

    // 数据源解密器
    protected final DataSourceDecipher dataSourceDecipher;


    public MultiDataSourceAutoConfiguration(MybatisFlexProperties properties
        , ObjectProvider<DataSourceDecipher> dataSourceDecipherProvider
    ) {
        dataSourceProperties = properties.getDatasource();
        dataSourceDecipher = dataSourceDecipherProvider.getIfAvailable();
        seataConfig = properties.getSeataConfig();
        master = properties.getDefaultDatasourceKey();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {

        FlexDataSource flexDataSource = null;

        if (dataSourceProperties != null && !dataSourceProperties.isEmpty()) {

            if (dataSourceDecipher != null) {
                DataSourceManager.setDecipher(dataSourceDecipher);
            }

            if (master != null) {
                Map<String, String> map = dataSourceProperties.remove(master);
                if (map != null) {
                    flexDataSource = addDataSource(MapUtil.entry(master, map), flexDataSource);
                } else {
                    throw FlexExceptions.wrap("没有找到默认数据源 \"%s\" 对应的配置，请检查您的多数据源配置。", master);
                }
            }

            for (Map.Entry<String, Map<String, String>> entry : dataSourceProperties.entrySet()) {
                flexDataSource = addDataSource(entry, flexDataSource);
            }
        }

        return flexDataSource;
    }

    private FlexDataSource addDataSource(Map.Entry<String, Map<String, String>> entry, FlexDataSource flexDataSource) {
        DataSource dataSource = new DataSourceBuilder(entry.getValue()).build();
        DataSourceManager.decryptDataSource(dataSource);

        if (seataConfig != null && seataConfig.isEnable()) {
            if (seataConfig.getSeataMode() == MybatisFlexProperties.SeataMode.XA) {
                dataSource = new DataSourceProxyXA(dataSource);
            } else {
                dataSource = new DataSourceProxy(dataSource);
            }
        }

        if (flexDataSource == null) {
            flexDataSource = new FlexDataSource(entry.getKey(), dataSource, false);
        } else {
            flexDataSource.addDataSource(entry.getKey(), dataSource, false);
        }
        return flexDataSource;
    }


    /**
     * {@link com.mybatisflex.annotation.UseDataSource} 注解切换数据源切面。
     */
    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public DataSourceAdvice dataSourceAdvice() {
        return new DataSourceAdvice();
    }

}
