package com.timetable.service;

import com.timetable.entity.StudentOperationRecord;
import com.timetable.repository.StudentOperationRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentOperationRecordService {
    
    @Autowired
    private StudentOperationRecordRepository operationRecordRepository;
    
    /**
     * 获取所有操作记录（管理员用）
     */
    public List<StudentOperationRecord> getAllRecords() {
        return operationRecordRepository.findAll();
    }
    
    /**
     * 根据教练ID和机构ID获取所有操作记录
     */
    public List<StudentOperationRecord> getRecordsByCoachIdAndOrganizationId(Long coachId, Long organizationId) {
        return operationRecordRepository.findByCoachIdAndOrganizationId(coachId, organizationId);
    }
    
    /**
     * 根据ID获取操作记录
     */
    public StudentOperationRecord getRecordById(Long id) {
        return operationRecordRepository.findById(id);
    }
    
    /**
     * 更新操作记录
     */
    public StudentOperationRecord updateRecord(StudentOperationRecord record) {
        return operationRecordRepository.update(record);
    }
    
    /**
     * 删除操作记录
     */
    public void deleteRecord(Long id) {
        operationRecordRepository.deleteById(id);
    }
    
    /**
     * 保存操作记录
     */
    public StudentOperationRecord saveRecord(StudentOperationRecord record) {
        Long id = operationRecordRepository.save(record);
        record.setId(id);
        return record;
    }
}