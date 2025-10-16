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

    public UserSalarySetting findById(Long id) {
        return dsl.selectFrom(table(TABLE_NAME))
                .where(field("id").eq(id))
                .fetchOneInto(UserSalarySetting.class);
    }

    public Long save(UserSalarySetting setting) {
        return dsl.insertInto(table(TABLE_NAME))
                .set(field("user_id"), setting.getUserId())
                .set(field("base_salary"), setting.getBaseSalary() != null ? setting.getBaseSalary() : java.math.BigDecimal.ZERO)
                .set(field("social_security"), setting.getSocialSecurity() != null ? setting.getSocialSecurity() : java.math.BigDecimal.ZERO)
                .set(field("hourly_rate"), setting.getHourlyRate() != null ? setting.getHourlyRate() : java.math.BigDecimal.ZERO)
                .set(field("commission_rate"), setting.getCommissionRate() != null ? setting.getCommissionRate() : java.math.BigDecimal.ZERO)
                .set(field("created_at"), java.time.LocalDateTime.now())
                .set(field("updated_at"), java.time.LocalDateTime.now())
                .returningResult(field("id", Long.class))
                .fetchOne()
                .value1();
    }

    public void update(UserSalarySetting setting) {
        dsl.update(table(TABLE_NAME))
                .set(field("base_salary"), setting.getBaseSalary() != null ? setting.getBaseSalary() : java.math.BigDecimal.ZERO)
                .set(field("social_security"), setting.getSocialSecurity() != null ? setting.getSocialSecurity() : java.math.BigDecimal.ZERO)
                .set(field("hourly_rate"), setting.getHourlyRate() != null ? setting.getHourlyRate() : java.math.BigDecimal.ZERO)
                .set(field("commission_rate"), setting.getCommissionRate() != null ? setting.getCommissionRate() : java.math.BigDecimal.ZERO)
                .set(field("updated_at"), java.time.LocalDateTime.now())
                .where(field("id").eq(setting.getId()))
                .execute();
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
}

