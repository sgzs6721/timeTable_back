package com.timetable.service;

import com.timetable.dto.StudentMergeDTO;
import com.timetable.dto.StudentMergeRequest;
import com.timetable.repository.StudentMergeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class StudentMergeService {
    
    private static final Logger logger = LoggerFactory.getLogger(StudentMergeService.class);
    
    @Autowired
    private StudentMergeRepository studentMergeRepository;
    
    public List<StudentMergeDTO> getMergesByCoach(Long coachId) {
        return studentMergeRepository.findByCoachId(coachId);
    }
    
    public StudentMergeDTO createMerge(Long coachId, StudentMergeRequest request) {
        StudentMergeDTO merge = new StudentMergeDTO();
        merge.setCoachId(coachId);
        merge.setDisplayName(request.getDisplayName());
        merge.setStudentNames(request.getStudentNames());
        
        Long id = studentMergeRepository.save(merge);
        merge.setId(id);
        return merge;
    }
    
    public StudentMergeDTO updateMerge(Long id, StudentMergeRequest request) {
        StudentMergeDTO merge = studentMergeRepository.findById(id);
        if (merge == null) {
            throw new IllegalArgumentException("合并记录不存在");
        }
        
        merge.setDisplayName(request.getDisplayName());
        merge.setStudentNames(request.getStudentNames());
        studentMergeRepository.update(merge);
        return merge;
    }
    
    public void deleteMerge(Long id) {
        StudentMergeDTO merge = studentMergeRepository.findById(id);
        if (merge == null) {
            throw new IllegalArgumentException("合并记录不存在");
        }
        studentMergeRepository.softDelete(id);
    }
}

