package com.timetable.repository;

import com.timetable.entity.UserSalarySetting;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class UserSalarySettingRepository {

    @Autowired
    private DSLContext dsl;

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
            org.jooq.Record1<Long> result = dsl.insertInto(table(TABLE_NAME))
                    .set(field("user_id"), setting.getUserId())
                    .set(field("organization_id"), setting.getOrganizationId())
                    .set(field("base_salary"), setting.getBaseSalary() != null ? setting.getBaseSalary() : java.math.BigDecimal.ZERO)
                    .set(field("social_security"), setting.getSocialSecurity() != null ? setting.getSocialSecurity() : java.math.BigDecimal.ZERO)
                    .set(field("hourly_rate"), setting.getHourlyRate() != null ? setting.getHourlyRate() : java.math.BigDecimal.ZERO)
                    .set(field("commission_rate"), setting.getCommissionRate() != null ? setting.getCommissionRate() : java.math.BigDecimal.ZERO)
                    .set(field("created_at"), java.time.LocalDateTime.now())
                    .set(field("updated_at"), java.time.LocalDateTime.now())
                    .returningResult(field("id", Long.class))
                    .fetchOne();
            
            if (result == null) {
                throw new RuntimeException("插入工资设置记录失败，未返回结果");
            }
            
            Long id = result.value1();
            if (id == null) {
                throw new RuntimeException("插入工资设置记录失败，未获取到ID");
            }
            
            return id;
        } catch (Exception e) {
            throw new RuntimeException("保存工资设置到数据库失败: " + e.getMessage(), e);
        }
    }

    public void update(UserSalarySetting setting) {
        try {
            if (setting.getId() == null) {
                throw new RuntimeException("更新工资设置失败：记录ID不能为空");
            }
            
            int rowsAffected = dsl.update(table(TABLE_NAME))
                    .set(field("base_salary"), setting.getBaseSalary() != null ? setting.getBaseSalary() : java.math.BigDecimal.ZERO)
                    .set(field("social_security"), setting.getSocialSecurity() != null ? setting.getSocialSecurity() : java.math.BigDecimal.ZERO)
                    .set(field("hourly_rate"), setting.getHourlyRate() != null ? setting.getHourlyRate() : java.math.BigDecimal.ZERO)
                    .set(field("commission_rate"), setting.getCommissionRate() != null ? setting.getCommissionRate() : java.math.BigDecimal.ZERO)
                    .set(field("updated_at"), java.time.LocalDateTime.now())
                    .where(field("id").eq(setting.getId()))
                    .execute();
                    
            if (rowsAffected == 0) {
                throw new RuntimeException("更新工资设置失败：未找到ID为 " + setting.getId() + " 的记录");
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

