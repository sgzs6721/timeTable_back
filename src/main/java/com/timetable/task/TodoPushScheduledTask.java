package com.timetable.task;

import com.timetable.config.WechatMpConfig;
import com.timetable.dto.NotificationSettingsDTO;
import com.timetable.dto.wechat.WechatTemplateMessage;
import com.timetable.dto.wechat.WechatTemplateMessageResponse;
import com.timetable.entity.Todo;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.TodoRepository;
import com.timetable.repository.UserRepository;
import com.timetable.service.OrganizationService;
import com.timetable.service.WechatMpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 待办推送定时任务
 */
@Component
public class TodoPushScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(TodoPushScheduledTask.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WechatMpService wechatMpService;

    @Autowired
    private WechatMpConfig wechatMpConfig;

    @Autowired
    private com.timetable.repository.CustomerRepository customerRepository;

    @Autowired
    private OrganizationService organizationService;

    /**
     * 每5分钟扫描一次需要推送的待办
     * cron表达式: 0 * /5 * * * * 表示每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void pushTodoReminders() {
        // 如果未启用推送功能，直接返回
        if (!wechatMpConfig.getMp().getEnabled()) {
            return;
        }

        logger.info("开始执行定时任务：扫描待办并推送微信提醒");
        
        try {
            // 获取最大重试次数
            int maxRetryCount = wechatMpConfig.getMp().getPushRetryMax();
            
            // 查询需要推送的待办
            List<Todo> todosToPush = todoRepository.findTodosToPush(maxRetryCount);
            
            if (todosToPush.isEmpty()) {
                logger.debug("没有需要推送的待办");
                return;
            }

            logger.info("找到 {} 条待办需要推送", todosToPush.size());

            int successCount = 0;
            int failCount = 0;

            for (Todo todo : todosToPush) {
                try {
                    // 获取创建待办的用户信息
                    Users user = userRepository.findById(todo.getCreatedBy());
                    if (user == null) {
                        logger.warn("待办 ID: {} 的创建用户不存在，跳过推送", todo.getId());
                        todoRepository.updatePushFailed(todo.getId(), "用户不存在");
                        failCount++;
                        continue;
                    }

                    // 检查用户所在机构的待办事项提醒设置
                    if (user.getOrganizationId() != null) {
                        try {
                            NotificationSettingsDTO notificationSettings = organizationService.getNotificationSettings(user.getOrganizationId());
                            if (notificationSettings.getTodoEnabled() == null || !notificationSettings.getTodoEnabled()) {
                                logger.debug("用户 {} 所在机构的待办事项提醒已关闭，跳过推送待办 ID: {}", user.getUsername(), todo.getId());
                                todoRepository.updatePushFailed(todo.getId(), "机构已关闭待办事项提醒");
                                failCount++;
                                continue;
                            }
                        } catch (Exception e) {
                            logger.warn("检查机构通知设置失败: {}", e.getMessage());
                            // 如果检查设置失败，继续推送
                        }
                    }

                    // 检查用户是否绑定了微信
                    if (user.getWechatOpenid() == null || user.getWechatOpenid().isEmpty()) {
                        logger.warn("用户 {} 未绑定微信，跳过推送待办 ID: {}", user.getUsername(), todo.getId());
                        todoRepository.updatePushFailed(todo.getId(), "用户未绑定微信");
                        failCount++;
                        continue;
                    }

                    // 构建提醒时间字符串（格式：YYYY-MM-DD HH:mm）
                    String reminderTime = "";
                    if (todo.getReminderDate() != null) {
                        reminderTime = todo.getReminderDate().toString();
                        if (todo.getReminderTime() != null) {
                            reminderTime += " " + todo.getReminderTime().toString().substring(0, 5);
                        }
                    }

                    // 获取客户电话（从 customers 表查询）
                    String customerPhone = null;
                    if (todo.getCustomerId() != null) {
                        try {
                            com.timetable.entity.Customer customer = customerRepository.findById(todo.getCustomerId());
                            if (customer != null && customer.getParentPhone() != null && !customer.getParentPhone().isEmpty()) {
                                customerPhone = customer.getParentPhone();
                            }
                        } catch (Exception e) {
                            logger.debug("无法获取客户电话: {}", e.getMessage());
                        }
                    }

                    // 构建模板消息
                    WechatTemplateMessage message = wechatMpService.buildTodoReminderMessage(
                        user.getWechatOpenid(),
                        todo.getCustomerName(),
                        todo.getContent(),
                        reminderTime,
                        customerPhone
                    );

                    // 发送模板消息
                    WechatTemplateMessageResponse response = wechatMpService.sendTemplateMessage(message);

                    if (response != null && response.isSuccess()) {
                        // 推送成功
                        todoRepository.updatePushSuccess(todo.getId());
                        successCount++;
                        logger.info("待办 ID: {} 推送成功，msgid: {}", todo.getId(), response.getMsgid());
                    } else {
                        // 推送失败
                        String errorMsg = response != null ? 
                            String.format("errcode: %d, errmsg: %s", response.getErrcode(), response.getErrmsg()) : 
                            "响应为空";
                        todoRepository.updatePushFailed(todo.getId(), errorMsg);
                        failCount++;
                        logger.error("待办 ID: {} 推送失败: {}", todo.getId(), errorMsg);
                    }

                } catch (Exception e) {
                    // 推送异常
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.length() > 500) {
                        errorMsg = errorMsg.substring(0, 500);
                    }
                    todoRepository.updatePushFailed(todo.getId(), errorMsg);
                    failCount++;
                    logger.error("待办 ID: {} 推送异常", todo.getId(), e);
                }

                // 避免推送过快，每条消息间隔100ms
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            logger.info("定时任务执行完成：成功推送 {} 条，失败 {} 条", successCount, failCount);

        } catch (Exception e) {
            logger.error("定时任务执行失败：扫描待办推送时发生错误", e);
        }
    }

    /**
     * 手动触发推送（用于测试）
     */
    public void manualPushTodoReminders() {
        logger.info("手动触发待办推送任务");
        pushTodoReminders();
    }
}

