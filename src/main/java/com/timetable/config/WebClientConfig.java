package com.timetable.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() throws SSLException {
        // 配置连接池
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();

        // 配置SSL/TLS
        SslContext sslContext = SslContextBuilder
                .forClient()
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .secure(t -> t.sslContext(sslContext)) // 应用SSL配置
                .responseTimeout(Duration.ofSeconds(120)); // 增加超时到120秒

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 增加缓冲区大小
                .build();
    }
} 