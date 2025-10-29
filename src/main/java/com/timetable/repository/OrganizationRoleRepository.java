package com.timetable.repository;

import com.timetable.entity.OrganizationRole;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrganizationRoleRepository extends BaseRepository {

    public List<OrganizationRole> findByOrganizationId(Long organizationId) {
        String sql = "SELECT * FROM organization_roles WHERE organization_id = ? ORDER BY created_at ASC";
        Result<Record> result = dsl.fetch(sql, organizationId);
        return result.into(OrganizationRole.class);
    }

    public Optional<OrganizationRole> findById(Long id) {
        String sql = "SELECT * FROM organization_roles WHERE id = ?";
        Result<Record> result = dsl.fetch(sql, id);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0).into(OrganizationRole.class));
    }

    public Optional<OrganizationRole> findByOrganizationIdAndRoleCode(Long organizationId, String roleCode) {
        String sql = "SELECT * FROM organization_roles WHERE organization_id = ? AND role_code = ?";
        Result<Record> result = dsl.fetch(sql, organizationId, roleCode);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0).into(OrganizationRole.class));
    }

    public OrganizationRole save(OrganizationRole role) {
        if (role.getId() == null) {
            // 插入
            String insertSql = "INSERT INTO organization_roles (organization_id, role_code, role_name, description, icon, color, is_system) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            dsl.execute(insertSql,
                    role.getOrganizationId(),
                    role.getRoleCode(),
                    role.getRoleName(),
                    role.getDescription(),
                    role.getIcon(),
                    role.getColor(),
                    role.getIsSystem() != null ? role.getIsSystem() : false);

            // 获取插入后的记录
            String selectSql = "SELECT * FROM organization_roles WHERE organization_id = ? AND role_code = ?";
            Result<Record> result = dsl.fetch(selectSql, role.getOrganizationId(), role.getRoleCode());
            return result.get(0).into(OrganizationRole.class);
        } else {
            // 更新
            String updateSql = "UPDATE organization_roles SET role_name = ?, description = ?, icon = ?, color = ? WHERE id = ?";
            dsl.execute(updateSql,
                    role.getRoleName(),
                    role.getDescription(),
                    role.getIcon(),
                    role.getColor(),
                    role.getId());
            return role;
        }
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM organization_roles WHERE id = ? AND is_system = FALSE";
        dsl.execute(sql, id);
    }

    public boolean existsByOrganizationIdAndRoleCode(Long organizationId, String roleCode) {
        String sql = "SELECT COUNT(*) FROM organization_roles WHERE organization_id = ? AND role_code = ?";
        Integer count = dsl.fetchOne(sql, organizationId, roleCode).into(Integer.class);
        return count != null && count > 0;
    }
}

