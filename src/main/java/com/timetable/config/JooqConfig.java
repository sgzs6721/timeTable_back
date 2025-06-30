package com.timetable.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * jOOQ配置类
 */
@Configuration
public class JooqConfig {

    @Bean
    public org.jooq.Configuration jooqConfiguration(DataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setSQLDialect(SQLDialect.MYSQL);
        return configuration;
    }

    @Bean
    public DSLContext dslContext(org.jooq.Configuration jooqConfiguration) {
        return DSL.using(jooqConfiguration);
    }
} 