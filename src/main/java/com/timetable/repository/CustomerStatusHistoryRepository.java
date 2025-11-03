package com.timetable.repository;

import com.timetable.entity.CustomerStatusHistory;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class CustomerStatusHistoryRepository extends BaseRepository {

    @Autowired
    private DSLContext dsl;

    public CustomerStatusHistory save(CustomerStatusHistory history) {
        try {
            dsl.insertInto(table("customer_status_history"))
                    .set(field("customer_id"), history.getCustomerId())
                    .set(field("from_status"), history.getFromStatus())
                    .set(field("to_status"), history.getToStatus())
                    .set(field("notes"), history.getNotes())
                    .set(field("created_by"), history.getCreatedBy())
                    .set(field("created_at"), LocalDateTime.now())
                    .set(field("organization_id"), history.getOrganizationId())
                    .set(field("trial_schedule_date"), history.getTrialScheduleDate())
                    .set(field("trial_start_time"), history.getTrialStartTime())
                    .set(field("trial_end_time"), history.getTrialEndTime())
                    .set(field("trial_coach_id"), history.getTrialCoachId())
                    .set(field("trial_student_name"), history.getTrialStudentName())
                    .execute();

            // 获取最后插入的ID
            Long id = dsl.select(field("LAST_INSERT_ID()", Long.class))
                    .fetchOne()
                    .value1();

            return findById(id);
        } catch (Exception e) {
            throw new RuntimeException("保存状态历史记录失败: " + e.getMessage(), e);
        }
    }

    public CustomerStatusHistory findById(Long id) {
        return dsl.select()
                .from(table("customer_status_history"))
                .where(field("id").eq(id))
                .fetchOne(record -> {
                    CustomerStatusHistory history = new CustomerStatusHistory();
                    history.setId(record.get(field("id", Long.class)));
                    history.setCustomerId(record.get(field("customer_id", Long.class)));
                    history.setFromStatus(record.get(field("from_status", String.class)));
                    history.setToStatus(record.get(field("to_status", String.class)));
                    history.setNotes(record.get(field("notes", String.class)));
                    history.setCreatedBy(record.get(field("created_by", Long.class)));
                    history.setOrganizationId(record.get(field("organization_id", Long.class)));
                    
                    // 处理Timestamp到LocalDateTime的转换
                    Object createdAtObj = record.get(field("created_at"));
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        history.setCreatedAt(((java.sql.Timestamp) createdAtObj).toLocalDateTime());
                    } else if (createdAtObj instanceof LocalDateTime) {
                        history.setCreatedAt((LocalDateTime) createdAtObj);
                    }
                    
                    // 读取体验课程字段
                    history.setTrialScheduleDate(record.get(field("trial_schedule_date", java.sql.Date.class)) != null ? 
                        record.get(field("trial_schedule_date", java.sql.Date.class)).toLocalDate() : null);
                    history.setTrialStartTime(record.get(field("trial_start_time", java.sql.Time.class)) != null ?
                        record.get(field("trial_start_time", java.sql.Time.class)).toLocalTime() : null);
                    history.setTrialEndTime(record.get(field("trial_end_time", java.sql.Time.class)) != null ?
                        record.get(field("trial_end_time", java.sql.Time.class)).toLocalTime() : null);
                    history.setTrialCoachId(record.get(field("trial_coach_id", Long.class)));
                    history.setTrialStudentName(record.get(field("trial_student_name", String.class)));
                    
                    // 读取体验课表信息字段
                    history.setTrialScheduleId(record.get(field("trial_schedule_id", Long.class)));
                    history.setTrialTimetableId(record.get(field("trial_timetable_id", Long.class)));
                    history.setTrialSourceType(record.get(field("trial_source_type", String.class)));
                    
                    // 读取体验取消状态字段
                    Object trialCancelledObj = record.get(field("trial_cancelled"));
                    if (trialCancelledObj != null) {
                        if (trialCancelledObj instanceof Boolean) {
                            history.setTrialCancelled((Boolean) trialCancelledObj);
                        } else if (trialCancelledObj instanceof Number) {
                            history.setTrialCancelled(((Number) trialCancelledObj).intValue() == 1);
                        }
                    }
                    
                    return history;
                });
    }

    public List<CustomerStatusHistory> findByCustomerId(Long customerId) {
        return dsl.select()
                .from(table("customer_status_history"))
                .where(field("customer_id").eq(customerId))
                .orderBy(field("created_at").desc())
                .fetch(record -> {
                    CustomerStatusHistory history = new CustomerStatusHistory();
                    history.setId(record.get(field("id", Long.class)));
                    history.setCustomerId(record.get(field("customer_id", Long.class)));
                    history.setFromStatus(record.get(field("from_status", String.class)));
                    history.setToStatus(record.get(field("to_status", String.class)));
                    history.setNotes(record.get(field("notes", String.class)));
                    history.setCreatedBy(record.get(field("created_by", Long.class)));
                    history.setOrganizationId(record.get(field("organization_id", Long.class)));
                    
                    // 处理Timestamp到LocalDateTime的转换
                    Object createdAtObj = record.get(field("created_at"));
                    if (createdAtObj instanceof java.sql.Timestamp) {
                        history.setCreatedAt(((java.sql.Timestamp) createdAtObj).toLocalDateTime());
                    } else if (createdAtObj instanceof LocalDateTime) {
                        history.setCreatedAt((LocalDateTime) createdAtObj);
                    }
                    
                    // 读取体验课程字段
                    history.setTrialScheduleDate(record.get(field("trial_schedule_date", java.sql.Date.class)) != null ? 
                        record.get(field("trial_schedule_date", java.sql.Date.class)).toLocalDate() : null);
                    history.setTrialStartTime(record.get(field("trial_start_time", java.sql.Time.class)) != null ?
                        record.get(field("trial_start_time", java.sql.Time.class)).toLocalTime() : null);
                    history.setTrialEndTime(record.get(field("trial_end_time", java.sql.Time.class)) != null ?
                        record.get(field("trial_end_time", java.sql.Time.class)).toLocalTime() : null);
                    history.setTrialCoachId(record.get(field("trial_coach_id", Long.class)));
                    history.setTrialStudentName(record.get(field("trial_student_name", String.class)));
                    
                    // 读取体验课表信息字段
                    history.setTrialScheduleId(record.get(field("trial_schedule_id", Long.class)));
                    history.setTrialTimetableId(record.get(field("trial_timetable_id", Long.class)));
                    history.setTrialSourceType(record.get(field("trial_source_type", String.class)));
                    
                    // 读取体验取消状态字段
                    Object trialCancelledObj = record.get(field("trial_cancelled"));
                    if (trialCancelledObj != null) {
                        if (trialCancelledObj instanceof Boolean) {
                            history.setTrialCancelled((Boolean) trialCancelledObj);
                        } else if (trialCancelledObj instanceof Number) {
                            history.setTrialCancelled(((Number) trialCancelledObj).intValue() == 1);
                        }
                    }
                    
                    return history;
                });
    }

    public CustomerStatusHistory update(CustomerStatusHistory history) {
        try {
            dsl.update(table("customer_status_history"))
                    .set(field("notes"), history.getNotes())
                    .where(field("id").eq(history.getId()))
                    .execute();
            
            return findById(history.getId());
        } catch (Exception e) {
            throw new RuntimeException("更新状态历史记录失败: " + e.getMessage(), e);
        }
    }

    public void delete(Long id) {
        try {
            dsl.deleteFrom(table("customer_status_history"))
                    .where(field("id").eq(id))
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("删除状态历史记录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 标记体验课程为已取消
     */
    public boolean markTrialAsCancelled(Long historyId) {
        try {
            int updated = dsl.update(table("customer_status_history"))
                    .set(field("trial_cancelled"), true)
                    .where(field("id").eq(historyId))
                    .execute();
            return updated > 0;
        } catch (Exception e) {
            throw new RuntimeException("标记体验课程取消失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新体验课程的课表信息（scheduleId, timetableId, sourceType）
     */
    public void updateTrialScheduleInfo(Long historyId, Long scheduleId, Long timetableId, String sourceType) {
        try {
            dsl.update(table("customer_status_history"))
                    .set(field("trial_schedule_id"), scheduleId)
                    .set(field("trial_timetable_id"), timetableId)
                    .set(field("trial_source_type"), sourceType)
                    .where(field("id").eq(historyId))
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("更新体验课程课表信息失败: " + e.getMessage(), e);
        }
    }
}

