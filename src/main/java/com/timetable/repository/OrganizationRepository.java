package com.timetable.repository;

import com.timetable.entity.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 机构数据访问层
 */
@Repository
public class OrganizationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Organization> organizationRowMapper = new RowMapper<Organization>() {
        @Override
        public Organization mapRow(ResultSet rs, int rowNum) throws SQLException {
            Organization organization = new Organization();
            organization.setId(rs.getLong("id"));
            organization.setName(rs.getString("name"));
            organization.setCode(rs.getString("code"));
            organization.setAddress(rs.getString("address"));
            organization.setContactPhone(rs.getString("contact_phone"));
            organization.setContactPerson(rs.getString("contact_person"));
            organization.setStatus(rs.getString("status"));
            organization.setSettings(rs.getString("settings"));
            organization.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            organization.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return organization;
        }
    };

    public Organization save(Organization organization) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        String sql = "INSERT INTO organizations (name, code, address, contact_phone, contact_person, status, settings, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, 
            organization.getName(),
            organization.getCode(),
            organization.getAddress(),
            organization.getContactPhone(),
            organization.getContactPerson(),
            organization.getStatus() != null ? organization.getStatus() : "ACTIVE",
            organization.getSettings(),
            now,
            now
        );
        
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        organization.setId(id);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);
        return organization;
    }

    public Organization update(Organization organization) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        String sql = "UPDATE organizations SET name = ?, code = ?, address = ?, contact_phone = ?, " +
                     "contact_person = ?, status = ?, settings = ?, updated_at = ? WHERE id = ?";
        
        jdbcTemplate.update(sql,
            organization.getName(),
            organization.getCode(),
            organization.getAddress(),
            organization.getContactPhone(),
            organization.getContactPerson(),
            organization.getStatus(),
            organization.getSettings(),
            now,
            organization.getId()
        );
        
        organization.setUpdatedAt(now);
        return organization;
    }

    public Organization findById(Long id) {
        String sql = "SELECT * FROM organizations WHERE id = ?";
        List<Organization> organizations = jdbcTemplate.query(sql, organizationRowMapper, id);
        return organizations.isEmpty() ? null : organizations.get(0);
    }

    public Organization findByCode(String code) {
        String sql = "SELECT * FROM organizations WHERE code = ?";
        List<Organization> organizations = jdbcTemplate.query(sql, organizationRowMapper, code);
        return organizations.isEmpty() ? null : organizations.get(0);
    }

    public List<Organization> findAll() {
        String sql = "SELECT * FROM organizations ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, organizationRowMapper);
    }

    public List<Organization> findByStatus(String status) {
        String sql = "SELECT * FROM organizations WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, organizationRowMapper, status);
    }

    public List<Organization> findActiveOrganizations() {
        String sql = "SELECT * FROM organizations WHERE status = 'ACTIVE' ORDER BY name ASC";
        return jdbcTemplate.query(sql, organizationRowMapper);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM organizations WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM organizations WHERE code = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code);
        return count != null && count > 0;
    }

    public boolean existsByCodeAndNotId(String code, Long id) {
        String sql = "SELECT COUNT(*) FROM organizations WHERE code = ? AND id != ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code, id);
        return count != null && count > 0;
    }
}

