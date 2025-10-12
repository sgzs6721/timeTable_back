package com.timetable.service;

import com.timetable.dto.StudentOperationRequest;
import com.timetable.dto.StudentAliasDTO;
import com.timetable.repository.StudentAliasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class StudentOperationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentOperationService.class);
    
    @Autowired
    private StudentAliasRepository studentAliasRepository;
    
    /**
     * 重命名学员
     */
    public void renameStudent(Long coachId, StudentOperationRequest request) {
        // 这里可以实现重命名逻辑
        // 由于不影响课表记录，主要是更新显示名称
        logger.info("重命名学员: {} -> {}", request.getOldName(), request.getNewName());
    }
    
    /**
     * 删除学员（软删除，不影响课表记录）
     */
    public void deleteStudent(Long coachId, String studentName) {
        // 这里可以实现删除逻辑
        // 由于不影响课表记录，主要是标记为不显示
        logger.info("删除学员: {}", studentName);
    }
    
    /**
     * 为学员分配别名
     */
    public StudentAliasDTO assignAlias(Long coachId, StudentOperationRequest request) {
        StudentAliasDTO alias = new StudentAliasDTO();
        alias.setCoachId(coachId);
        alias.setAliasName(request.getAliasName());
        alias.setStudentNames(List.of(request.getOldName()));
        
        Long id = studentAliasRepository.save(alias);
        alias.setId(id);
        return alias;
    }
}
