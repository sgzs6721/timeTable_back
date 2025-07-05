package com.timetable.repository;

import org.springframework.stereotype.Repository;
import org.jooq.DSLContext;
import com.timetable.generated.tables.pojos.Timetables;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 课表Repository - jOOQ实现
 */
@Repository
public class TimetableRepository {
    
    @Autowired
    private DSLContext dsl;
    
    /**
     * 根据用户ID查找课表列表
     */
    public List<com.timetable.generated.tables.pojos.Timetables> findByUserId(Long userId) {
        return dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.USER_ID.eq(userId))
                .fetchInto(com.timetable.generated.tables.pojos.Timetables.class);
    }
    
    /**
     * 根据ID查找课表
     */
    public com.timetable.generated.tables.pojos.Timetables findById(Long id) {
        return dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.eq(id))
                .fetchOneInto(com.timetable.generated.tables.pojos.Timetables.class);
    }
    
    /**
     * 根据ID和用户ID查找课表
     */
    public com.timetable.generated.tables.pojos.Timetables findByIdAndUserId(Long id, Long userId) {
        return dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.eq(id)
                        .and(com.timetable.generated.tables.Timetables.TIMETABLES.USER_ID.eq(userId)))
                .fetchOneInto(com.timetable.generated.tables.pojos.Timetables.class);
    }
    
    /**
     * 保存或更新课表
     */
    public com.timetable.generated.tables.pojos.Timetables save(com.timetable.generated.tables.pojos.Timetables timetable) {
        if (timetable.getId() == null) {
            // 新增
            return dsl.insertInto(com.timetable.generated.tables.Timetables.TIMETABLES)
                    .set(dsl.newRecord(com.timetable.generated.tables.Timetables.TIMETABLES, timetable))
                    .returning()
                    .fetchOne()
                    .into(com.timetable.generated.tables.pojos.Timetables.class);
        } else {
            // 更新
            dsl.update(com.timetable.generated.tables.Timetables.TIMETABLES)
                    .set(dsl.newRecord(com.timetable.generated.tables.Timetables.TIMETABLES, timetable))
                    .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.eq(timetable.getId()))
                    .execute();
            return findById(timetable.getId());
        }
    }
    
    /**
     * 删除课表
     */
    public void deleteById(Long id) {
        dsl.deleteFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.eq(id))
                .execute();
    }
    
    /**
     * 检查课表是否存在
     */
    public boolean existsById(Long id) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.eq(id))
        );
    }
    
    /**
     * 检查课表是否属于指定用户
     */
    public boolean existsByIdAndUserId(Long id, Long userId) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.eq(id)
                        .and(com.timetable.generated.tables.Timetables.TIMETABLES.USER_ID.eq(userId)))
        );
    }
    
    /**
     * 获取所有课表（管理员功能）
     */
    public List<com.timetable.generated.tables.pojos.Timetables> findAll() {
        return dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .fetchInto(com.timetable.generated.tables.pojos.Timetables.class);
    }
    
    /**
     * 根据ID列表查找课表
     */
    public List<com.timetable.generated.tables.pojos.Timetables> findByIdIn(List<Long> ids) {
        return dsl.selectFrom(com.timetable.generated.tables.Timetables.TIMETABLES)
                .where(com.timetable.generated.tables.Timetables.TIMETABLES.ID.in(ids))
                .fetchInto(com.timetable.generated.tables.pojos.Timetables.class);
    }
} 