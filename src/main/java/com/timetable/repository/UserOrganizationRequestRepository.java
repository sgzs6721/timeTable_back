package com.timetable.repository;

import com.timetable.entity.Organization;
import com.timetable.entity.UserOrganizationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户机构申请数据访问层
 */
@Repository
public class UserOrganizationRequestRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<UserOrganizationRequest> requestRowMapper = new RowMapper<UserOrganizationRequest>() {
        @Override
        public UserOrganizationRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserOrganizationRequest request = new UserOrganizationRequest();
            request.setId(rs.getLong("id"));
            request.setUserId(rs.getLong("user_id") != 0 ? rs.getLong("user_id") : null);
            request.setOrganizationId(rs.getLong("organization_id"));
            request.setWechatOpenid(rs.getString("wechat_openid"));
            request.setWechatUnionid(rs.getString("wechat_unionid"));
            request.setWechatNickname(rs.getString("wechat_nickname"));
            request.setWechatAvatar(rs.getString("wechat_avatar"));
            request.setWechatSex(rs.getByte("wechat_sex"));
            request.setApplyReason(rs.getString("apply_reason"));
            request.setStatus(rs.getString("status"));
            request.setApprovedBy(rs.getLong("approved_by") != 0 ? rs.getLong("approved_by") : null);
            request.setApprovedAt(rs.getTimestamp("approved_at") != null ? 
                rs.getTimestamp("approved_at").toLocalDateTime() : null);
            request.setRejectReason(rs.getString("reject_reason"));
            request.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            request.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return request;
        }
    };

    private final RowMapper<UserOrganizationRequest> requestWithOrgRowMapper = new RowMapper<UserOrganizationRequest>() {
        @Override
        public UserOrganizationRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserOrganizationRequest request = new UserOrganizationRequest();
            request.setId(rs.getLong("id"));
            request.setUserId(rs.getLong("user_id") != 0 ? rs.getLong("user_id") : null);
            request.setOrganizationId(rs.getLong("organization_id"));
            request.setWechatOpenid(rs.getString("wechat_openid"));
            request.setWechatUnionid(rs.getString("wechat_unionid"));
            request.setWechatNickname(rs.getString("wechat_nickname"));
            request.setWechatAvatar(rs.getString("wechat_avatar"));
            request.setWechatSex(rs.getByte("wechat_sex"));
            request.setApplyReason(rs.getString("apply_reason"));
            request.setStatus(rs.getString("status"));
            request.setApprovedBy(rs.getLong("approved_by") != 0 ? rs.getLong("approved_by") : null);
            request.setApprovedAt(rs.getTimestamp("approved_at") != null ? 
                rs.getTimestamp("approved_at").toLocalDateTime() : null);
            request.setRejectReason(rs.getString("reject_reason"));
            request.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            request.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            
            // 机构信息
            Organization org = new Organization();
            org.setId(rs.getLong("org_id"));
            org.setName(rs.getString("org_name"));
            org.setCode(rs.getString("org_code"));
            org.setAddress(rs.getString("org_address"));
            org.setContactPhone(rs.getString("org_contact_phone"));
            org.setStatus(rs.getString("org_status"));
            request.setOrganization(org);
            
            // 审批人信息
            request.setApprovedByUsername(rs.getString("approved_by_username"));
            
            return request;
        }
    };

    public UserOrganizationRequest save(UserOrganizationRequest request) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        String sql = "INSERT INTO user_organization_requests (user_id, organization_id, wechat_openid, wechat_unionid, " +
                     "wechat_nickname, wechat_avatar, wechat_sex, apply_reason, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, 
            request.getUserId(),
            request.getOrganizationId(),
            request.getWechatOpenid(),
            request.getWechatUnionid(),
            request.getWechatNickname(),
            request.getWechatAvatar(),
            request.getWechatSex(),
            request.getApplyReason(),
            request.getStatus() != null ? request.getStatus() : "PENDING",
            now,
            now
        );
        
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        request.setId(id);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return request;
    }

    public UserOrganizationRequest update(UserOrganizationRequest request) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        String sql = "UPDATE user_organization_requests SET user_id = ?, status = ?, approved_by = ?, " +
                     "approved_at = ?, reject_reason = ?, updated_at = ? WHERE id = ?";
        
        jdbcTemplate.update(sql,
            request.getUserId(),
            request.getStatus(),
            request.getApprovedBy(),
            request.getApprovedAt(),
            request.getRejectReason(),
            now,
            request.getId()
        );
        
        request.setUpdatedAt(now);
        return request;
    }

    public UserOrganizationRequest findById(Long id) {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "WHERE r.id = ?";
        List<UserOrganizationRequest> requests = jdbcTemplate.query(sql, requestWithOrgRowMapper, id);
        return requests.isEmpty() ? null : requests.get(0);
    }

    public UserOrganizationRequest findByWechatOpenid(String wechatOpenid) {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "WHERE r.wechat_openid = ? " +
                     "ORDER BY r.created_at DESC LIMIT 1";
        List<UserOrganizationRequest> requests = jdbcTemplate.query(sql, requestWithOrgRowMapper, wechatOpenid);
        return requests.isEmpty() ? null : requests.get(0);
    }

    public List<UserOrganizationRequest> findByOrganizationId(Long organizationId) {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "WHERE r.organization_id = ? " +
                     "ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, requestWithOrgRowMapper, organizationId);
    }

    public List<UserOrganizationRequest> findByStatus(String status) {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username, " +
                     "user.position as user_position " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "LEFT JOIN users user ON r.user_id = user.id " +
                     "WHERE r.status = ? " +
                     "ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, requestWithOrgRowMapper, status);
    }

    public List<UserOrganizationRequest> findByOrganizationIdAndStatus(Long organizationId, String status) {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "WHERE r.organization_id = ? AND r.status = ? " +
                     "ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, requestWithOrgRowMapper, organizationId, status);
    }

    public List<UserOrganizationRequest> findAll() {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, requestWithOrgRowMapper);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM user_organization_requests WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public boolean existsByWechatOpenidAndStatus(String wechatOpenid, String status) {
        String sql = "SELECT COUNT(*) FROM user_organization_requests WHERE wechat_openid = ? AND status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, wechatOpenid, status);
        return count != null && count > 0;
    }

    public boolean existsByWechatOpenidAndOrganizationIdAndStatus(String wechatOpenid, Long organizationId, String status) {
        String sql = "SELECT COUNT(*) FROM user_organization_requests WHERE wechat_openid = ? AND organization_id = ? AND status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, wechatOpenid, organizationId, status);
        return count != null && count > 0;
    }

    public UserOrganizationRequest findByWechatOpenidAndOrganizationIdAndStatus(String wechatOpenid, Long organizationId, String status) {
        String sql = "SELECT r.*, " +
                     "o.id as org_id, o.name as org_name, o.code as org_code, o.address as org_address, " +
                     "o.contact_phone as org_contact_phone, o.status as org_status, " +
                     "u.username as approved_by_username " +
                     "FROM user_organization_requests r " +
                     "LEFT JOIN organizations o ON r.organization_id = o.id " +
                     "LEFT JOIN users u ON r.approved_by = u.id " +
                     "WHERE r.wechat_openid = ? AND r.organization_id = ? AND r.status = ? " +
                     "ORDER BY r.created_at DESC LIMIT 1";
        List<UserOrganizationRequest> requests = jdbcTemplate.query(sql, requestWithOrgRowMapper, wechatOpenid, organizationId, status);
        return requests.isEmpty() ? null : requests.get(0);
    }
}

