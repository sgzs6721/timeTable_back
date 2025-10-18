package com.timetable.repository;

import com.timetable.entity.SalarySystemSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SalarySystemSettingRepository extends BaseRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<SalarySystemSetting> rowMapper = (rs, rowNum) -> {
        SalarySystemSetting setting = new SalarySystemSetting();
        setting.setId(rs.getLong("id"));
        setting.setSalaryStartDay(rs.getInt("salary_start_day"));
        setting.setSalaryEndDay(rs.getInt("salary_end_day"));
        setting.setSalaryPayDay(rs.getInt("salary_pay_day"));
        setting.setDescription(rs.getString("description"));
        setting.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        setting.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return setting;
    };

    /**
     * 获取当前的工资系统设置（只会有一条记录）
     */
    public SalarySystemSetting getCurrentSetting() {
        String sql = "SELECT * FROM salary_system_settings ORDER BY id DESC LIMIT 1";
        try {
            List<SalarySystemSetting> settings = jdbcTemplate.query(sql, rowMapper);
            return settings.isEmpty() ? null : settings.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 保存工资系统设置
     */
    public Long save(SalarySystemSetting setting) {
        String sql = "INSERT INTO salary_system_settings (salary_start_day, salary_end_day, salary_pay_day, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, setting.getSalaryStartDay());
            ps.setInt(2, setting.getSalaryEndDay());
            ps.setInt(3, setting.getSalaryPayDay());
            ps.setString(4, setting.getDescription());
            ps.setObject(5, setting.getCreatedAt());
            ps.setObject(6, setting.getUpdatedAt());
            return ps;
        }, keyHolder);
        
        return keyHolder.getKey().longValue();
    }

    /**
     * 更新工资系统设置
     */
    public void update(SalarySystemSetting setting) {
        String sql = "UPDATE salary_system_settings SET salary_start_day = ?, salary_end_day = ?, salary_pay_day = ?, description = ?, updated_at = ? WHERE id = ?";
        
        jdbcTemplate.update(sql, 
            setting.getSalaryStartDay(),
            setting.getSalaryEndDay(),
            setting.getSalaryPayDay(),
            setting.getDescription(),
            setting.getUpdatedAt(),
            setting.getId()
        );
    }

    /**
     * 保存或更新设置（如果已存在则更新，否则创建新的）
     */
    public SalarySystemSetting saveOrUpdate(SalarySystemSetting setting) {
        SalarySystemSetting existing = getCurrentSetting();
        LocalDateTime now = LocalDateTime.now();
        
        if (existing != null) {
            // 更新现有记录
            setting.setId(existing.getId());
            setting.setCreatedAt(existing.getCreatedAt());
            setting.setUpdatedAt(now);
            update(setting);
            return findById(existing.getId());
        } else {
            // 创建新记录
            setting.setCreatedAt(now);
            setting.setUpdatedAt(now);
            Long id = save(setting);
            return findById(id);
        }
    }

    /**
     * 根据ID查找设置
     */
    public SalarySystemSetting findById(Long id) {
        String sql = "SELECT * FROM salary_system_settings WHERE id = ?";
        try {
            List<SalarySystemSetting> settings = jdbcTemplate.query(sql, rowMapper, id);
            return settings.isEmpty() ? null : settings.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
