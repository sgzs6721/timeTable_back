package com.timetable.repository;

import com.timetable.entity.StudentNames;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentNamesRepository {
    
    @Autowired
    private DSLContext dsl;
    
    /**
     * 保存或更新学生姓名记录
     */
    public void saveOrUpdate(String name, Long userId) {
        // 检查是否已存在
        StudentNames existing = dsl.selectFrom(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                .where(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME.eq(name))
                .and(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USER_ID.eq(userId))
                .fetchOneInto(StudentNames.class);
        
        if (existing != null) {
            // 更新使用次数和最后使用时间
            dsl.update(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USAGE_COUNT, 
                         com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USAGE_COUNT.add(1))
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.LAST_USED_AT, 
                         java.time.LocalDateTime.now())
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.UPDATED_AT, 
                         java.time.LocalDateTime.now())
                    .where(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.ID.eq(existing.getId()))
                    .execute();
        } else {
            // 创建新记录
            dsl.insertInto(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME, name)
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USER_ID, userId)
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USAGE_COUNT, 1)
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.FIRST_USED_AT, 
                         java.time.LocalDateTime.now())
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.LAST_USED_AT, 
                         java.time.LocalDateTime.now())
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.CREATED_AT, 
                         java.time.LocalDateTime.now())
                    .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.UPDATED_AT, 
                         java.time.LocalDateTime.now())
                    .execute();
        }
    }
    
    /**
     * 根据输入前缀搜索学生姓名
     */
    public List<String> searchNamesByPrefix(String prefix, Long userId, int limit) {
        return dsl.select(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME)
                .from(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                .where(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME.like(prefix + "%"))
                .and(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USER_ID.eq(userId))
                .orderBy(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USAGE_COUNT.desc(),
                         com.timetable.generated.tables.StudentNames.STUDENT_NAMES.LAST_USED_AT.desc())
                .limit(limit)
                .fetchInto(String.class);
    }
    
    /**
     * 获取用户的所有学生姓名（按使用频率排序）
     */
    public List<String> getAllNamesByUserId(Long userId, int limit) {
        return dsl.select(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME)
                .from(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                .where(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USER_ID.eq(userId))
                .orderBy(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USAGE_COUNT.desc(),
                         com.timetable.generated.tables.StudentNames.STUDENT_NAMES.LAST_USED_AT.desc())
                .limit(limit)
                .fetchInto(String.class);
    }
    
    /**
     * 根据名称和用户ID查找学员ID，如果不存在则创建
     */
    public Long findOrCreateStudentId(String name, Long userId) {
        // 先查找是否存在
        Long existingId = dsl.select(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.ID)
                .from(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                .where(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME.eq(name.trim()))
                .and(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USER_ID.eq(userId))
                .fetchOneInto(Long.class);
        
        if (existingId != null) {
            return existingId;
        }
        
        // 不存在则创建
        Long newId = dsl.insertInto(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.NAME, name.trim())
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USER_ID, userId)
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.USAGE_COUNT, 1)
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.FIRST_USED_AT, 
                     java.time.LocalDateTime.now())
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.LAST_USED_AT, 
                     java.time.LocalDateTime.now())
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.CREATED_AT, 
                     java.time.LocalDateTime.now())
                .set(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.UPDATED_AT, 
                     java.time.LocalDateTime.now())
                .returningResult(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.ID)
                .fetchOne()
                .getValue(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.ID);
        
        return newId;
    }
    
    /**
     * 根据学员ID获取学员信息
     */
    public StudentNames findById(Long studentId) {
        return dsl.selectFrom(com.timetable.generated.tables.StudentNames.STUDENT_NAMES)
                .where(com.timetable.generated.tables.StudentNames.STUDENT_NAMES.ID.eq(studentId))
                .fetchOneInto(StudentNames.class);
    }
} 