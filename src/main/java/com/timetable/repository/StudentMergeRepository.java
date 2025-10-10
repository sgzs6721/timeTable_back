package com.timetable.repository;

import com.timetable.dto.StudentMergeDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StudentMergeRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    public StudentMergeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    public List<StudentMergeDTO> findByCoachId(Long coachId) {
        String sql = "SELECT * FROM student_merges WHERE coach_id = ? AND is_deleted = 0 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new StudentMergeRowMapper(), coachId);
    }
    
    public StudentMergeDTO findById(Long id) {
        String sql = "SELECT * FROM student_merges WHERE id = ? AND is_deleted = 0";
        List<StudentMergeDTO> results = jdbcTemplate.query(sql, new StudentMergeRowMapper(), id);
        return results.isEmpty() ? null : results.get(0);
    }
    
    public Long save(StudentMergeDTO merge) {
        String sql = "INSERT INTO student_merges (display_name, student_names, coach_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, merge.getDisplayName(), 
                objectMapper.writeValueAsString(merge.getStudentNames()),
                merge.getCoachId(), LocalDateTime.now(), LocalDateTime.now());
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } catch (Exception e) {
            throw new RuntimeException("保存学员合并失败", e);
        }
    }
    
    public void update(StudentMergeDTO merge) {
        String sql = "UPDATE student_merges SET display_name = ?, student_names = ?, updated_at = ? WHERE id = ?";
        try {
            jdbcTemplate.update(sql, merge.getDisplayName(),
                objectMapper.writeValueAsString(merge.getStudentNames()),
                LocalDateTime.now(), merge.getId());
        } catch (Exception e) {
            throw new RuntimeException("更新学员合并失败", e);
        }
    }
    
    public void softDelete(Long id) {
        String sql = "UPDATE student_merges SET is_deleted = 1, deleted_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
    
    private class StudentMergeRowMapper implements RowMapper<StudentMergeDTO> {
        @Override
        public StudentMergeDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            StudentMergeDTO dto = new StudentMergeDTO();
            dto.setId(rs.getLong("id"));
            dto.setDisplayName(rs.getString("display_name"));
            dto.setCoachId(rs.getLong("coach_id"));
            dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            dto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            
            try {
                String studentNamesJson = rs.getString("student_names");
                List<String> studentNames = objectMapper.readValue(studentNamesJson, new TypeReference<List<String>>() {});
                dto.setStudentNames(studentNames);
            } catch (Exception e) {
                dto.setStudentNames(new java.util.ArrayList<>());
            }
            
            return dto;
        }
    }
}
