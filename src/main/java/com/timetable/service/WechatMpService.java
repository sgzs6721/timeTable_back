package com.timetable.service;

import com.timetable.config.WechatMpConfig;
import com.timetable.dto.wechat.WechatAccessTokenResponse;
import com.timetable.dto.wechat.WechatTemplateMessage;
import com.timetable.dto.wechat.WechatTemplateMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class WechatMpService {

    private static final Logger logger = LoggerFactory.getLogger(WechatMpService.class);

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String TEMPLATE_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";

    @Autowired
    private WechatMpConfig wechatMpConfig;

    @Autowired
    private WebClient webClient;

    // 微信 API WebClient
    private WebClient wechatApiClient;

    // Access Token 缓存
    private String cachedAccessToken;
    private LocalDateTime tokenExpireTime;

    public WechatMpService() {
        // 创建专门用于微信 API 的 WebClient
        this.wechatApiClient = WebClient.builder()
                .baseUrl("https://api.weixin.qq.com")
                .build();
    }

    /**
     * 获取 Access Token
     */
    public String getAccessToken() {
        // 如果启用了缓存且 token 未过期，直接返回缓存的 token
        if (wechatMpConfig.getMp().getTokenCacheEnabled() 
            && cachedAccessToken != null 
            && tokenExpireTime != null 
            && LocalDateTime.now().isBefore(tokenExpireTime)) {
            logger.debug("使用缓存的 Access Token");
            return cachedAccessToken;
        }

        try {
            logger.info("开始获取微信 Access Token");
            
            WechatAccessTokenResponse response = wechatApiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/token")
                            .queryParam("grant_type", "client_credential")
                            .queryParam("appid", wechatMpConfig.getAppId())
                            .queryParam("secret", wechatMpConfig.getAppSecret())
                            .build())
                    .retrieve()
                    .bodyToMono(WechatAccessTokenResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.isSuccess()) {
                cachedAccessToken = response.getAccessToken();
                // 设置过期时间为配置的缓存时间
                tokenExpireTime = LocalDateTime.now().plusSeconds(
                    wechatMpConfig.getMp().getTokenCacheTime()
                );
                logger.info("成功获取 Access Token，过期时间: {}", tokenExpireTime);
                return cachedAccessToken;
            } else {
                String errorMsg = response != null ? 
                    String.format("errcode: %d, errmsg: %s", response.getErrcode(), response.getErrmsg()) : 
                    "响应为空";
                logger.error("获取 Access Token 失败: {}", errorMsg);
                throw new RuntimeException("获取微信 Access Token 失败: " + errorMsg);
            }
        } catch (Exception e) {
            logger.error("获取 Access Token 异常", e);
            throw new RuntimeException("获取微信 Access Token 异常: " + e.getMessage(), e);
        }
    }

    /**
     * 发送模板消息
     */
    public WechatTemplateMessageResponse sendTemplateMessage(WechatTemplateMessage message) {
        if (!wechatMpConfig.getMp().getEnabled()) {
            logger.warn("微信模板消息推送未启用");
            return null;
        }

        try {
            String accessToken = getAccessToken();
            
            logger.info("开始发送模板消息到用户: {}", message.getToUser());
            
            WechatTemplateMessageResponse response = wechatApiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/message/template/send")
                            .queryParam("access_token", accessToken)
                            .build())
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(WechatTemplateMessageResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.isSuccess()) {
                logger.info("模板消息发送成功，msgid: {}", response.getMsgid());
            } else {
                String errorMsg = response != null ? 
                    String.format("errcode: %d, errmsg: %s", response.getErrcode(), response.getErrmsg()) : 
                    "响应为空";
                logger.error("模板消息发送失败: {}", errorMsg);
            }

            return response;
        } catch (Exception e) {
            logger.error("发送模板消息异常", e);
            throw new RuntimeException("发送模板消息异常: " + e.getMessage(), e);
        }
    }

    /**
     * 构建待办提醒模板消息
     * 模板字段：
     * - thing16: 客户姓名
     * - phone_number10: 联系方式
     * - thing17: 项目名称（待办内容）
     * - time23: 发布时间（提醒时间）
     * - thing20: 工单信息
     */
    public WechatTemplateMessage buildTodoReminderMessage(
            String openid,
            String customerName,
            String content,
            String reminderTime,
            String customerPhone) {
        
        WechatTemplateMessage message = new WechatTemplateMessage();
        message.setToUser(openid);
        message.setTemplateId(wechatMpConfig.getMp().getTemplateId());
        
        // thing16 字段：客户姓名（限制20字）
        String customerNameValue = customerName != null && !customerName.isEmpty() ? customerName : "暂无";
        if (customerNameValue.length() > 20) {
            customerNameValue = customerNameValue.substring(0, 17) + "...";
        }
        message.addData("thing16", customerNameValue, "#173177");
        
        // phone_number10 字段：联系方式（电话号码格式）
        String phoneValue = customerPhone != null && !customerPhone.isEmpty() ? customerPhone : "暂无";
        message.addData("phone_number10", phoneValue, "#173177");
        
        // thing17 字段：项目名称/待办内容（限制20字）
        String contentValue = content != null && !content.isEmpty() ? content : "待办事项";
        if (contentValue.length() > 20) {
            contentValue = contentValue.substring(0, 17) + "...";
        }
        message.addData("thing17", contentValue, "#173177");
        
        // time23 字段：发布时间/提醒时间（时间格式：YYYY-MM-DD HH:mm）
        String timeValue = reminderTime != null && !reminderTime.isEmpty() ? reminderTime : 
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        message.addData("time23", timeValue, "#173177");
        
        // thing20 字段：工单信息（限制20字）
        message.addData("thing20", "待办提醒", "#173177");
        
        return message;
    }

    /**
     * 清除缓存的 Access Token（用于 token 失效时强制刷新）
     */
    public void clearAccessToken() {
        logger.info("清除缓存的 Access Token");
        cachedAccessToken = null;
        tokenExpireTime = null;
    }
}

