package com.timetable.dto;

/**
 * 通知设置DTO
 */
public class NotificationSettingsDTO {
    private Boolean wechatEnabled;
    private Boolean scheduleChangeEnabled;
    private Boolean customerNewEnabled;
    private Boolean todoEnabled;
    private Boolean paymentPendingEnabled;

    public NotificationSettingsDTO() {
    }

    public NotificationSettingsDTO(Boolean wechatEnabled, Boolean scheduleChangeEnabled, 
                                  Boolean customerNewEnabled, Boolean todoEnabled, 
                                  Boolean paymentPendingEnabled) {
        this.wechatEnabled = wechatEnabled;
        this.scheduleChangeEnabled = scheduleChangeEnabled;
        this.customerNewEnabled = customerNewEnabled;
        this.todoEnabled = todoEnabled;
        this.paymentPendingEnabled = paymentPendingEnabled;
    }

    public Boolean getWechatEnabled() {
        return wechatEnabled;
    }

    public void setWechatEnabled(Boolean wechatEnabled) {
        this.wechatEnabled = wechatEnabled;
    }

    public Boolean getScheduleChangeEnabled() {
        return scheduleChangeEnabled;
    }

    public void setScheduleChangeEnabled(Boolean scheduleChangeEnabled) {
        this.scheduleChangeEnabled = scheduleChangeEnabled;
    }

    public Boolean getCustomerNewEnabled() {
        return customerNewEnabled;
    }

    public void setCustomerNewEnabled(Boolean customerNewEnabled) {
        this.customerNewEnabled = customerNewEnabled;
    }

    public Boolean getTodoEnabled() {
        return todoEnabled;
    }

    public void setTodoEnabled(Boolean todoEnabled) {
        this.todoEnabled = todoEnabled;
    }

    public Boolean getPaymentPendingEnabled() {
        return paymentPendingEnabled;
    }

    public void setPaymentPendingEnabled(Boolean paymentPendingEnabled) {
        this.paymentPendingEnabled = paymentPendingEnabled;
    }
}