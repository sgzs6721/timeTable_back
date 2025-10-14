package com.timetable.repository;

import com.timetable.entity.StudentOperationRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

/**
 * 学员操作记录数据访问层
 */
@Repository
public class StudentOperationRecordRepository extends BaseRepository {

    @Autowired
    private DSLContext dsl;

    /**
     * 保存学员操作记录
     */
    public Long save(StudentOperationRecord record) {
        if (record.getId() == null) {
            // 创建新记录 - 使用JOOQ的returningResult来获取生成的ID
            Record result = dsl.insertInto(table("student_operation_records"))
                    .set(field("coach_id"), record.getCoachId())
                    .set(field("operation_type"), record.getOperationType())
                    .set(field("old_name"), record.getOldName())
                    .set(field("new_name"), record.getNewName())
                    .set(field("details"), record.getDetails())
                    .set(field("created_at"), record.getCreatedAt())
                    .set(field("updated_at"), record.getUpdatedAt())
                    .returningResult(field("id"))
                    .fetchOne();
            
            if (result != null) {
                Long generatedId = result.get("id", Long.class);
                record.setId(generatedId);
                System.out.println("DEBUG: 成功插入记录，ID: " + generatedId + ", 教练ID: " + record.getCoachId() + ", 操作类型: " + record.getOperationType() + ", 原名: " + record.getOldName() + ", 新名: " + record.getNewName());
                return generatedId;
            } else {
                System.out.println("ERROR: 插入记录失败，result为null");
                return null;
            }
        } else {
            // 更新现有记录
            int affectedRows = dsl.update(table("student_operation_records"))
                    .set(field("coach_id"), record.getCoachId())
                    .set(field("operation_type"), record.getOperationType())
                    .set(field("old_name"), record.getOldName())
                    .set(field("new_name"), record.getNewName())
                    .set(field("details"), record.getDetails())
                    .set(field("updated_at"), LocalDateTime.now())
                    .where(field("id").eq(record.getId()))
                    .execute();
            
            System.out.println("DEBUG: 更新记录，ID: " + record.getId() + ", 影响行数: " + affectedRows);
            return record.getId();
        }
    }

    /**
     * 根据教练ID查找所有操作记录
     */
    public List<StudentOperationRecord> findByCoachId(Long coachId) {
        Result<Record> records = dsl.select()
                .from(table("student_operation_records"))
                .where(field("coach_id").eq(coachId))
                .orderBy(field("created_at").desc())
                .fetch();
        
        return records.map(this::mapToStudentOperationRecord);
    }

    /**
     * 根据ID查找操作记录
     */
    public StudentOperationRecord findById(Long id) {
        Record record = dsl.select()
                .from(table("student_operation_records"))
                .where(field("id").eq(id))
                .fetchOne();
        
        return record != null ? mapToStudentOperationRecord(record) : null;
    }

    /**
     * 根据教练ID、操作类型和原名称查找操作记录
     */
    public StudentOperationRecord findByCoachIdAndOperationTypeAndOldName(Long coachId, String operationType, String oldName) {
        Record record = dsl.select()
                .from(table("student_operation_records"))
                .where(field("coach_id").eq(coachId))
                .and(field("operation_type").eq(operationType))
                .and(field("old_name").eq(oldName))
                .fetchOne();
        
        return record != null ? mapToStudentOperationRecord(record) : null;
    }

    /**
     * 根据ID删除操作记录
     */
    public void deleteById(Long id) {
        dsl.deleteFrom(table("student_operation_records"))
                .where(field("id").eq(id))
                .execute();
    }

    /**
     * 更新操作记录
     */
    public StudentOperationRecord update(StudentOperationRecord record) {
        dsl.update(table("student_operation_records"))
                .set(field("old_name"), record.getOldName())
                .set(field("new_name"), record.getNewName())
                .set(field("details"), record.getDetails())
                .set(field("updated_at"), LocalDateTime.now())
                .where(field("id").eq(record.getId()))
                .execute();
        
        return findById(record.getId());
    }

    /**
     * 映射Record到StudentOperationRecord
     */
    private StudentOperationRecord mapToStudentOperationRecord(Record record) {
        StudentOperationRecord operationRecord = new StudentOperationRecord();
        operationRecord.setId(record.get("id", Long.class));
        operationRecord.setCoachId(record.get("coach_id", Long.class));
        operationRecord.setOperationType(record.get("operation_type", String.class));
        operationRecord.setOldName(record.get("old_name", String.class));
        operationRecord.setNewName(record.get("new_name", String.class));
        operationRecord.setDetails(record.get("details", String.class));
        operationRecord.setCreatedAt(record.get("created_at", LocalDateTime.class));
        operationRecord.setUpdatedAt(record.get("updated_at", LocalDateTime.class));
        return operationRecord;
    }
}