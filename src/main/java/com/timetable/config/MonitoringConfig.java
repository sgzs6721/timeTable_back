package com.timetable.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JVM监控配置类
 * 配置Micrometer指标收集和Prometheus导出
 */
@Configuration
public class MonitoringConfig {

    /**
     * 自定义MeterRegistry，添加应用标签
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", "timetable-backend")
                .commonTags("version", "1.0.0");
    }

    /**
     * 配置指标过滤器，过滤不需要的指标
     */
    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.accept();
    }
}

