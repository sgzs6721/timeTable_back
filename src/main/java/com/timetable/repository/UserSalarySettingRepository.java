package com.timetable.repository;

import com.timetable.entity.UserSalarySetting;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class UserSalarySettingRepository {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "user_salary_settings";

    public List<UserSalarySetting> findAll() {
        return dsl.selectFrom(table(TABLE_NAME))
                .fetchInto(UserSalarySetting.class);
    }

    public UserSalarySetting findByUserId(Long userId) {
        return dsl.selectFrom(table(TABLE_NAME))
                .where(field("user_id").eq(userId))
                .fetchOneInto(UserSalarySetting.class);
    }

    public UserSalarySetting findByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return dsl.selectFrom(table(TABLE_NAME))
                .where(field("user_id").eq(userId)
                        .and(field("organization_id").eq(organizationId)))
                .fetchOneInto(UserSalarySetting.class);
    }

    public List<UserSalarySetting> findByOrganizationId(Long organizationId) {
        return dsl.selectFrom(table(TABLE_NAME))
                .where(field("organization_id").eq(organizationId))
                .fetchInto(UserSalarySetting.class);
    }

    public UserSalarySetting findById(Long id) {
        return dsl.selectFrom(table(TABLE_NAME))
                .where(field("id").eq(id))
                .fetchOneInto(UserSalarySetting.class);
    }

    public Long save(UserSalarySetting setting) {
        try {
            String sql = "INSERT INTO user_salary_settings (user_id, organization_id, base_salary, social_security, hourly_rate, commission_rate, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, setting.getUserId());
                ps.setLong(2, setting.getOrganizationId());
                ps.setBigDecimal(3, setting.getBaseSalary() != null ? setting.getBaseSalary() : java.math.BigDecimal.ZERO);
                ps.setBigDecimal(4, setting.getSocialSecurity() != null ? setting.getSocialSecurity() : java.math.BigDecimal.ZERO);
                ps.setBigDecimal(5, setting.getHourlyRate() != null ? setting.getHourlyRate() : java.math.BigDecimal.ZERO);
                ps.setBigDecimal(6, setting.getCommissionRate() != null ? setting.getCommissionRate() : java.math.BigDecimal.ZERO);
                ps.setObject(7, setting.getCreatedAt());
                ps.setObject(8, setting.getUpdatedAt());
                return ps;
            }, keyHolder);
            
            if (keyHolder.getKey() == null) {
                throw new RuntimeException("插入工资设置记录失败，未获取到ID");
            }
            
            return keyHolder.getKey().longValue();
        } catch (Exception e) {
            throw new RuntimeException("保存工资设置到数据库失败: " + e.getMessage(), e);
        }
    }

    public void update(UserSalarySetting setting) {
        try {
            if (setting.getId() == null) {
                throw new RuntimeException("更新工资设置失败：记录ID不能为空");
            }
            
            if (setting.getUserId() == null) {
                throw new RuntimeException("更新工资设置失败：用户ID不能为空");
            }
            
            if (setting.getOrganizationId() == null) {
                throw new RuntimeException("更新工资设置失败：机构ID不能为空");
            }
            
            int rowsAffected = dsl.update(table(TABLE_NAME))
                    .set(field("base_salary"), setting.getBaseSalary() != null ? setting.getBaseSalary() : java.math.BigDecimal.ZERO)
                    .set(field("social_security"), setting.getSocialSecurity() != null ? setting.getSocialSecurity() : java.math.BigDecimal.ZERO)
                    .set(field("hourly_rate"), setting.getHourlyRate() != null ? setting.getHourlyRate() : java.math.BigDecimal.ZERO)
                    .set(field("commission_rate"), setting.getCommissionRate() != null ? setting.getCommissionRate() : java.math.BigDecimal.ZERO)
                    .set(field("updated_at"), java.time.LocalDateTime.now())
                    .where(field("id").eq(setting.getId())
                            .and(field("user_id").eq(setting.getUserId()))
                            .and(field("organization_id").eq(setting.getOrganizationId())))
                    .execute();
                    
            if (rowsAffected == 0) {
                throw new RuntimeException("更新工资设置失败：未找到匹配的记录（ID=" + setting.getId() + 
                        ", userId=" + setting.getUserId() + ", organizationId=" + setting.getOrganizationId() + "）");
            }
        } catch (Exception e) {
            throw new RuntimeException("更新工资设置到数据库失败: " + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        dsl.deleteFrom(table(TABLE_NAME))
                .where(field("id").eq(id))
                .execute();
    }

    public void deleteByUserId(Long userId) {
        dsl.deleteFrom(table(TABLE_NAME))
                .where(field("user_id").eq(userId))
                .execute();
    }

    public void deleteByUserIdAndOrganizationId(Long userId, Long organizationId) {
        dsl.deleteFrom(table(TABLE_NAME))
                .where(field("user_id").eq(userId)
                        .and(field("organization_id").eq(organizationId)))
                .execute();
    }
}

