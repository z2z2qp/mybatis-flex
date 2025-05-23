/*
 *    Copyright 2015-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.mybatisflex.spring.boot;

import org.apache.ibatis.scripting.LanguageDriver;
import org.mybatis.scripting.freemarker.FreeMarkerLanguageDriver;
import org.mybatis.scripting.freemarker.FreeMarkerLanguageDriverConfig;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriverConfig;
import org.mybatis.scripting.velocity.VelocityLanguageDriver;
import org.mybatis.scripting.velocity.VelocityLanguageDriverConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 脚本语言驱动的自动配置，平常一般项目用不到，只为了同步 MyBatis 自带的 MybatisLanguageDriverAutoConfiguration。
 * <a href="https://github.com/mybatis/spring-boot-starter/blob/master/mybatis-spring-boot-autoconfigure/src/main/java/org/mybatis/spring/boot/autoconfigure/MybatisLanguageDriverAutoConfiguration.java">参考</a>
 * @author Kazuki Shimizu
 * @author michael
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(LanguageDriver.class)
public class MybatisLanguageDriverAutoConfiguration {

    private static final String CONFIGURATION_PROPERTY_PREFIX = "mybatis-flex.scripting-language-driver";

    /**
     * Configuration class for mybatis-freemarker 1.1.x or under.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(FreeMarkerLanguageDriver.class)
    @ConditionalOnMissingClass("org.mybatis.scripting.freemarker.FreeMarkerLanguageDriverConfig")
    public static class LegacyFreeMarkerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        FreeMarkerLanguageDriver freeMarkerLanguageDriver() {
            return new FreeMarkerLanguageDriver();
        }

    }

    /**
     * Configuration class for mybatis-freemarker 1.2.x or above.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({FreeMarkerLanguageDriver.class, FreeMarkerLanguageDriverConfig.class})
    public static class FreeMarkerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        FreeMarkerLanguageDriver freeMarkerLanguageDriver(FreeMarkerLanguageDriverConfig config) {
            return new FreeMarkerLanguageDriver(config);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties(CONFIGURATION_PROPERTY_PREFIX + ".freemarker")
        public FreeMarkerLanguageDriverConfig freeMarkerLanguageDriverConfig() {
            return FreeMarkerLanguageDriverConfig.newInstance();
        }

    }

    /**
     * Configuration class for mybatis-velocity 2.0 or under.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.mybatis.scripting.velocity.Driver.class)
    @ConditionalOnMissingClass("org.mybatis.scripting.velocity.VelocityLanguageDriverConfig")
    @SuppressWarnings("deprecation")
    public static class LegacyVelocityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        org.mybatis.scripting.velocity.Driver velocityLanguageDriver() {
            return new org.mybatis.scripting.velocity.Driver();
        }

    }

    /**
     * Configuration class for mybatis-velocity 2.1.x or above.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({VelocityLanguageDriver.class, VelocityLanguageDriverConfig.class})
    public static class VelocityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        VelocityLanguageDriver velocityLanguageDriver(VelocityLanguageDriverConfig config) {
            return new VelocityLanguageDriver(config);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties(CONFIGURATION_PROPERTY_PREFIX + ".velocity")
        public VelocityLanguageDriverConfig velocityLanguageDriverConfig() {
            return VelocityLanguageDriverConfig.newInstance();
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ThymeleafLanguageDriver.class)
    public static class ThymeleafConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ThymeleafLanguageDriver thymeleafLanguageDriver(ThymeleafLanguageDriverConfig config) {
            return new ThymeleafLanguageDriver(config);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties(CONFIGURATION_PROPERTY_PREFIX + ".thymeleaf")
        public ThymeleafLanguageDriverConfig thymeleafLanguageDriverConfig() {
            return ThymeleafLanguageDriverConfig.newInstance();
        }

        // This class provides to avoid the https://github.com/spring-projects/spring-boot/issues/21626 as workaround.
        @SuppressWarnings("unused")
        private static class MetadataThymeleafLanguageDriverConfig extends ThymeleafLanguageDriverConfig {

            @ConfigurationProperties(CONFIGURATION_PROPERTY_PREFIX + ".thymeleaf.dialect")
            @Override
            public DialectConfig getDialect() {
                return super.getDialect();
            }

            @ConfigurationProperties(CONFIGURATION_PROPERTY_PREFIX + ".thymeleaf.template-file")
            @Override
            public TemplateFileConfig getTemplateFile() {
                return super.getTemplateFile();
            }

        }

    }

}
