package com.timetable.task;

import com.timetable.service.WeeklyInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 周实例定时任务
 */
@Component
public class WeeklyInstanceScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyInstanceScheduledTask.class);

    @Autowired
    private WeeklyInstanceService weeklyInstanceService;

    // 已废弃：每周一1点生成本周实例（改为周日19点预生下周实例）

    /**
     * 每周日19:00预生成下周实例
     * 0 0 19 * * SUN 表示每周日的19:00:00执行
     */
    @Scheduled(cron = "0 0 19 * * SUN")
    public void generateNextWeekInstances() {
        logger.info("开始执行定时任务：为所有活动课表生成下周实例");
        try {
            weeklyInstanceService.generateNextWeekInstancesForAllActiveTimetables();
            logger.info("定时任务执行成功：生成下周实例");
        } catch (Exception e) {
            logger.error("定时任务执行失败：生成下周实例时发生错误", e);
        }
    }

    /**
     * 每天凌晨2点检查并生成当前周实例（防止遗漏）
     * cron: 0 0 2 * * * 表示每天的2:00:00执行
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyCheckAndGenerateWeeklyInstances() {
        logger.info("开始执行每日检查任务：确保当前周实例存在");
        
        try {
            weeklyInstanceService.generateCurrentWeekInstancesForAllActiveTimetables();
            logger.info("每日检查任务执行成功：当前周实例已确保存在");
        } catch (Exception e) {
            logger.error("每日检查任务执行失败：生成当前周实例时发生错误", e);
        }
    }

    /**
     * 手动触发生成当前周实例（用于测试或紧急情况）
     * 可以通过管理接口调用
     */
    public void manualGenerateWeeklyInstances() {
        logger.info("手动触发生成当前周实例任务");
        
        try {
            weeklyInstanceService.generateCurrentWeekInstancesForAllActiveTimetables();
            logger.info("手动生成当前周实例任务执行成功");
        } catch (Exception e) {
            logger.error("手动生成当前周实例任务执行失败", e);
            throw new RuntimeException("生成当前周实例失败: " + e.getMessage());
        }
    }
}
