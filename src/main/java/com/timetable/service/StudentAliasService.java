package com.timetable.service;

import com.timetable.dto.StudentAliasDTO;
import com.timetable.dto.StudentAliasRequest;
import com.timetable.repository.StudentAliasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class StudentAliasService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentAliasService.class);
    
    @Autowired
    private StudentAliasRepository studentAliasRepository;
    
    public List<StudentAliasDTO> getAliasesByCoach(Long coachId) {
        return studentAliasRepository.findByCoachId(coachId);
    }
    
    public StudentAliasDTO createAlias(Long coachId, StudentAliasRequest request) {
        StudentAliasDTO alias = new StudentAliasDTO();
        alias.setCoachId(coachId);
        alias.setAliasName(request.getAliasName());
        alias.setStudentNames(request.getStudentNames());
        
        Long id = studentAliasRepository.save(alias);
        alias.setId(id);
        return alias;
    }
    
    public StudentAliasDTO updateAlias(Long id, StudentAliasRequest request) {
        StudentAliasDTO alias = studentAliasRepository.findById(id);
        if (alias == null) {
            throw new IllegalArgumentException("别名记录不存在");
        }
        
        alias.setAliasName(request.getAliasName());
        alias.setStudentNames(request.getStudentNames());
        studentAliasRepository.update(alias);
        return alias;
    }
    
    public void deleteAlias(Long id) {
        StudentAliasDTO alias = studentAliasRepository.findById(id);
        if (alias == null) {
            throw new IllegalArgumentException("别名记录不存在");
        }
        studentAliasRepository.softDelete(id);
    }
}

