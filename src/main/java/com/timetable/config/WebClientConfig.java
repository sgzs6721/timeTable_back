package com.timetable.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import io.netty.channel.ChannelOption;
import reactor.netty.resources.LoopResources;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient webClient() throws SSLException {
        logger.info("创建 WebClient Bean...");
        
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
                .protocols("TLSv1.2", "TLSv1.3") // 明确指定支持的 TLS 版本
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // 临时禁用证书验证
                .build();

        logger.info("SSL 上下文配置完成，使用不安全的信任管理器");

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000) // 连接超时 120 秒
                .option(ChannelOption.SO_KEEPALIVE, true) // 启用 TCP keepalive
                .option(ChannelOption.TCP_NODELAY, true) // 禁用 Nagle 算法
                .runOn(LoopResources.create("custom-http-client")) // 使用自定义事件循环
                .secure(t -> t.sslContext(sslContext)) // 应用SSL配置
                .responseTimeout(Duration.ofSeconds(120)) // 增加超时到120秒
                .wiretap(true); // 启用详细的网络日志记录

        logger.info("HttpClient 配置完成");

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 增加缓冲区大小
                .build();
        
        logger.info("WebClient 创建完成");
        return client;
    }
} 