package com.timetable.repository;

import com.timetable.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Customer> customerRowMapper = new RowMapper<Customer>() {
        @Override
        public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Customer customer = new Customer();
            customer.setId(rs.getLong("id"));
            customer.setChildName(rs.getString("child_name"));
            customer.setGrade(rs.getString("grade"));
            customer.setParentPhone(rs.getString("parent_phone"));
            customer.setParentRelation(rs.getString("parent_relation"));
            customer.setAvailableTime(rs.getString("available_time"));
            customer.setSource(rs.getString("source"));
            customer.setStatus(rs.getString("status"));
            customer.setNotes(rs.getString("notes"));
            customer.setNextContactTime(rs.getTimestamp("next_contact_time") != null ? 
                rs.getTimestamp("next_contact_time").toLocalDateTime() : null);
            customer.setVisitTime(rs.getTimestamp("visit_time") != null ? 
                rs.getTimestamp("visit_time").toLocalDateTime() : null);
            customer.setAssignedSalesId(rs.getLong("assigned_sales_id") != 0 ? rs.getLong("assigned_sales_id") : null);
            customer.setOrganizationId(rs.getLong("organization_id") != 0 ? rs.getLong("organization_id") : null);
            customer.setCreatedBy(rs.getLong("created_by"));
            customer.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            customer.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return customer;
        }
    };

    public Customer save(Customer customer) {
        // 使用中国时区的当前时间
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        String sql = "INSERT INTO customers (child_name, grade, parent_phone, parent_relation, available_time, source, status, notes, next_contact_time, visit_time, assigned_sales_id, created_by, created_at, updated_at, organization_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, 
            customer.getChildName(),
            customer.getGrade(),
            customer.getParentPhone(),
            customer.getParentRelation(),
            customer.getAvailableTime(),
            customer.getSource(),
            customer.getStatus(),
            customer.getNotes(),
            customer.getNextContactTime(),
            customer.getVisitTime(),
            customer.getAssignedSalesId(),
            customer.getCreatedBy(),
            now,
            now,
            customer.getOrganizationId()
        );
        
        // 获取插入的ID
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        customer.setId(id);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        return customer;
    }

    public Customer update(Customer customer) {
        // 使用中国时区的当前时间
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        
        String sql = "UPDATE customers SET child_name = ?, grade = ?, parent_phone = ?, parent_relation = ?, " +
                     "available_time = ?, source = ?, status = ?, notes = ?, next_contact_time = ?, visit_time = ?, " +
                     "assigned_sales_id = ?, updated_at = ?, organization_id = ? WHERE id = ?";
        
        jdbcTemplate.update(sql,
            customer.getChildName(),
            customer.getGrade(),
            customer.getParentPhone(),
            customer.getParentRelation(),
            customer.getAvailableTime(),
            customer.getSource(),
            customer.getStatus(),
            customer.getNotes(),
            customer.getNextContactTime(),
            customer.getVisitTime(),
            customer.getAssignedSalesId(),
            now,
            customer.getOrganizationId(),
            customer.getId()
        );
        
        customer.setUpdatedAt(now);
        return customer;
    }

    public Customer findById(Long id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        List<Customer> customers = jdbcTemplate.query(sql, customerRowMapper, id);
        return customers.isEmpty() ? null : customers.get(0);
    }

    public List<Customer> findByAssignedSalesId(Long salesId) {
        String sql = "SELECT * FROM customers WHERE assigned_sales_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, customerRowMapper, salesId);
    }

    public List<Customer> findByAssignedSalesIdWithPagination(Long salesId, int page, int pageSize) {
        String sql = "SELECT * FROM customers WHERE assigned_sales_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, customerRowMapper, salesId, pageSize, page * pageSize);
    }

    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, customerRowMapper);
    }

    public List<Customer> findAllWithPagination(int page, int pageSize) {
        String sql = "SELECT * FROM customers ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, customerRowMapper, pageSize, page * pageSize);
    }

    public List<Customer> findAllWithFiltersAndPagination(int page, int pageSize, String status, Long salesId, 
                                                           java.time.LocalDate filterDate, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        
        if (salesId != null && salesId > 0) {
            sql.append(" AND assigned_sales_id = ?");
            params.add(salesId);
        }
        
        if (filterDate != null) {
            sql.append(" AND DATE(created_at) = ?");
            params.add(filterDate);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (child_name LIKE ? OR parent_phone LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(page * pageSize);
        
        return jdbcTemplate.query(sql.toString(), customerRowMapper, params.toArray());
    }

    public List<Customer> findByUserWithFiltersAndPagination(Long userId, int page, int pageSize, 
                                                              String status, java.time.LocalDate filterDate, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE assigned_sales_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        
        if (filterDate != null) {
            sql.append(" AND DATE(created_at) = ?");
            params.add(filterDate);
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (child_name LIKE ? OR parent_phone LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(page * pageSize);
        
        return jdbcTemplate.query(sql.toString(), customerRowMapper, params.toArray());
    }

    public List<Customer> findByStatus(String status) {
        String sql = "SELECT * FROM customers WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, customerRowMapper, status);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM customers WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Customer> findByCreatedBy(Long createdBy) {
        String sql = "SELECT * FROM customers WHERE created_by = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, customerRowMapper, createdBy);
    }

    public Customer findByChildName(String childName) {
        String sql = "SELECT * FROM customers WHERE child_name = ? ORDER BY created_at DESC LIMIT 1";
        List<Customer> customers = jdbcTemplate.query(sql, customerRowMapper, childName);
        return customers.isEmpty() ? null : customers.get(0);
    }

    public Customer findByChildNameLike(String childName) {
        String sql = "SELECT * FROM customers WHERE child_name LIKE ? ORDER BY created_at DESC LIMIT 1";
        List<Customer> customers = jdbcTemplate.query(sql, customerRowMapper, "%" + childName + "%");
        return customers.isEmpty() ? null : customers.get(0);
    }

    public List<Customer> findByOrganizationId(Long organizationId) {
        String sql = "SELECT * FROM customers WHERE organization_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, customerRowMapper, organizationId);
    }
}
