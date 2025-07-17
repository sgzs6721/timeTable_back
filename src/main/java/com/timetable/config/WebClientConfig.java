package com.timetable.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.protocol.HttpProtocol;
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

        // 配置SSL/TLS - 警告：这里使用了不安全的信任管理器，仅用于诊断目的
        // 这会跳过证书验证，不应在生产环境中使用
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // 临时禁用证书验证
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .protocol(HttpProtocol.H2C, HttpProtocol.HTTP11) // 启用 HTTP/2 和 HTTP/1.1
                .secure(t -> t.sslContext(sslContext)) // 应用SSL配置
                .responseTimeout(Duration.ofSeconds(120)); // 增加超时到120秒

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 增加缓冲区大小
                .build();
    }
} 