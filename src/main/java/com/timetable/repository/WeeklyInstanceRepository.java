package com.timetable.repository;

import com.timetable.entity.WeeklyInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * 周实例数据访问层
 */
@Repository
public class WeeklyInstanceRepository extends BaseRepository {

    @Autowired
    private DSLContext dsl;

    /**
     * 保存周实例
     */
    public WeeklyInstance save(WeeklyInstance instance) {
        if (instance.getId() == null) {
            // 创建新记录
            // 先插入记录
            int affectedRows = dsl.insertInto(table("weekly_instances"))
                    .set(field("template_timetable_id"), instance.getTemplateTimetableId())
                    .set(field("week_start_date"), instance.getWeekStartDate())
                    .set(field("week_end_date"), instance.getWeekEndDate())
                    .set(field("year_week"), instance.getYearWeek())
                    .set(field("is_current"), instance.getIsCurrent())
                    .set(field("generated_at"), instance.getGeneratedAt())
                    .set(field("last_synced_at"), instance.getLastSyncedAt())
                    .set(field("created_at"), instance.getCreatedAt())
                    .set(field("updated_at"), instance.getUpdatedAt())
                    .execute();
            
            if (affectedRows > 0) {
                // 查询刚插入的记录获取ID
                Record record = dsl.select(field("id"))
                        .from(table("weekly_instances"))
                        .where(field("template_timetable_id").eq(instance.getTemplateTimetableId()))
                        .and(field("year_week").eq(instance.getYearWeek()))
                        .orderBy(field("id").desc())
                        .limit(1)
                        .fetchOne();
                
                if (record != null) {
                    Long generatedId = record.get("id", Long.class);
                    instance.setId(generatedId);
                } else {
                    throw new RuntimeException("插入周实例记录成功但无法查询到ID");
                }
            } else {
                throw new RuntimeException("插入周实例记录失败，影响行数为0");
            }
        } else {
            // 更新现有记录
            dsl.update(table("weekly_instances"))
                    .set(field("template_timetable_id"), instance.getTemplateTimetableId())
                    .set(field("week_start_date"), instance.getWeekStartDate())
                    .set(field("week_end_date"), instance.getWeekEndDate())
                    .set(field("year_week"), instance.getYearWeek())
                    .set(field("is_current"), instance.getIsCurrent())
                    .set(field("generated_at"), instance.getGeneratedAt())
                    .set(field("last_synced_at"), instance.getLastSyncedAt())
                    .set(field("updated_at"), LocalDateTime.now())
                    .where(field("id").eq(instance.getId()))
                    .execute();
        }
        return instance;
    }

    /**
     * 根据ID查找周实例
     */
    public WeeklyInstance findById(Long id) {
        Record record = dsl.select()
                .from(table("weekly_instances"))
                .where(field("id").eq(id))
                .fetchOne();
        
        return record != null ? mapToWeeklyInstance(record) : null;
    }

    /**
     * 根据模板课表ID和年周查找周实例
     */
    public WeeklyInstance findByTemplateIdAndYearWeek(Long templateTimetableId, String yearWeek) {
        Record record = dsl.select()
                .from(table("weekly_instances"))
                .where(field("template_timetable_id").eq(templateTimetableId))
                .and(field("year_week").eq(yearWeek))
                .fetchOne();
        
        return record != null ? mapToWeeklyInstance(record) : null;
    }

    /**
     * 获取指定模板课表的所有周实例
     */
    public List<WeeklyInstance> findByTemplateTimetableId(Long templateTimetableId) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instances"))
                .where(field("template_timetable_id").eq(templateTimetableId))
                .orderBy(field("week_start_date").desc())
                .fetch();
        
        return records.map(this::mapToWeeklyInstance);
    }

    /**
     * 获取当前周实例
     */
    public List<WeeklyInstance> findCurrentWeekInstances() {
        Result<Record> records = dsl.select()
                .from(table("weekly_instances"))
                .where(field("is_current").eq(true))
                .fetch();
        
        return records.map(this::mapToWeeklyInstance);
    }

    /**
     * 获取指定模板课表的当前周实例
     */
    public WeeklyInstance findCurrentWeekInstanceByTemplateId(Long templateTimetableId) {
        Record record = dsl.select()
                .from(table("weekly_instances"))
                .where(field("template_timetable_id").eq(templateTimetableId))
                .and(field("is_current").eq(true))
                .fetchOne();
        
        return record != null ? mapToWeeklyInstance(record) : null;
    }

    /**
     * 清除所有当前周标记
     */
    public void clearAllCurrentWeekFlags() {
        dsl.update(table("weekly_instances"))
                .set(field("is_current"), false)
                .where(field("is_current").eq(true))
                .execute();
    }

    /**
     * 清除指定模板课表的当前周标记
     */
    public void clearCurrentWeekFlagByTemplateId(Long templateTimetableId) {
        dsl.update(table("weekly_instances"))
                .set(field("is_current"), false)
                .where(field("template_timetable_id").eq(templateTimetableId))
                .and(field("is_current").eq(true))
                .execute();
    }

    /**
     * 设置当前周实例
     */
    public void setCurrentWeekInstance(Long instanceId) {
        WeeklyInstance instance = findById(instanceId);
        if (instance != null) {
            // 先清除同一模板课表的其他当前周标记
            clearCurrentWeekFlagByTemplateId(instance.getTemplateTimetableId());
            
            // 设置当前实例为当前周
            dsl.update(table("weekly_instances"))
                    .set(field("is_current"), true)
                    .set(field("updated_at"), LocalDateTime.now())
                    .where(field("id").eq(instanceId))
                    .execute();
        }
    }

    /**
     * 根据日期范围查找周实例
     */
    public List<WeeklyInstance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        Result<Record> records = dsl.select()
                .from(table("weekly_instances"))
                .where(field("week_start_date").le(endDate))
                .and(field("week_end_date").ge(startDate))
                .orderBy(field("week_start_date"))
                .fetch();
        
        return records.map(this::mapToWeeklyInstance);
    }

    /**
     * 删除周实例
     */
    public void delete(Long id) {
        dsl.deleteFrom(table("weekly_instances"))
                .where(field("id").eq(id))
                .execute();
    }

    /**
     * 删除模板课表的所有周实例
     */
    public void deleteByTemplateTimetableId(Long templateTimetableId) {
        dsl.deleteFrom(table("weekly_instances"))
                .where(field("template_timetable_id").eq(templateTimetableId))
                .execute();
    }

    /**
     * 更新最后同步时间
     */
    public void updateLastSyncedAt(Long instanceId, LocalDateTime syncTime) {
        dsl.update(table("weekly_instances"))
                .set(field("last_synced_at"), syncTime)
                .set(field("updated_at"), LocalDateTime.now())
                .where(field("id").eq(instanceId))
                .execute();
    }

    /**
     * 检查周实例是否存在
     */
    public boolean exists(Long templateTimetableId, String yearWeek) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(table("weekly_instances"))
                        .where(field("template_timetable_id").eq(templateTimetableId))
                        .and(field("year_week").eq(yearWeek))
        );
    }

    /**
     * 映射Record到WeeklyInstance
     */
    private WeeklyInstance mapToWeeklyInstance(Record record) {
        WeeklyInstance instance = new WeeklyInstance();
        instance.setId(record.get("id", Long.class));
        instance.setTemplateTimetableId(record.get("template_timetable_id", Long.class));
        instance.setWeekStartDate(record.get("week_start_date", LocalDate.class));
        instance.setWeekEndDate(record.get("week_end_date", LocalDate.class));
        instance.setYearWeek(record.get("year_week", String.class));
        instance.setIsCurrent(record.get("is_current", Boolean.class));
        instance.setGeneratedAt(record.get("generated_at", LocalDateTime.class));
        instance.setLastSyncedAt(record.get("last_synced_at", LocalDateTime.class));
        instance.setCreatedAt(record.get("created_at", LocalDateTime.class));
        instance.setUpdatedAt(record.get("updated_at", LocalDateTime.class));
        return instance;
    }
}
