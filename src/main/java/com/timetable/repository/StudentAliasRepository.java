package com.timetable.repository;

import com.timetable.dto.StudentAliasDTO;
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
public class StudentAliasRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    public StudentAliasRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    public List<StudentAliasDTO> findByCoachId(Long coachId) {
        String sql = "SELECT * FROM student_aliases WHERE coach_id = ? AND is_deleted = 0 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new StudentAliasRowMapper(), coachId);
    }
    
    public StudentAliasDTO findById(Long id) {
        String sql = "SELECT * FROM student_aliases WHERE id = ? AND is_deleted = 0";
        List<StudentAliasDTO> results = jdbcTemplate.query(sql, new StudentAliasRowMapper(), id);
        return results.isEmpty() ? null : results.get(0);
    }
    
    public Long save(StudentAliasDTO alias) {
        String sql = "INSERT INTO student_aliases (alias_name, student_names, coach_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, alias.getAliasName(), 
            objectMapper.writeValueAsString(alias.getStudentNames()),
            alias.getCoachId(), LocalDateTime.now(), LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    
    public void update(StudentAliasDTO alias) {
        String sql = "UPDATE student_aliases SET alias_name = ?, student_names = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, alias.getAliasName(),
            objectMapper.writeValueAsString(alias.getStudentNames()),
            LocalDateTime.now(), alias.getId());
    }
    
    public void softDelete(Long id) {
        String sql = "UPDATE student_aliases SET is_deleted = 1, deleted_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
    
    private class StudentAliasRowMapper implements RowMapper<StudentAliasDTO> {
        @Override
        public StudentAliasDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            StudentAliasDTO dto = new StudentAliasDTO();
            dto.setId(rs.getLong("id"));
            dto.setAliasName(rs.getString("alias_name"));
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
