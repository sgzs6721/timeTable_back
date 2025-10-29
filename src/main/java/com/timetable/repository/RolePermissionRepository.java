package com.timetable.repository;

import com.timetable.entity.RolePermission;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色权限Repository
 */
@Repository
public class RolePermissionRepository extends BaseRepository {

    @Autowired
    public RolePermissionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * 根据机构ID获取所有角色权限
     */
    public List<RolePermission> findByOrganizationId(Long organizationId) {
        String sql = "SELECT * FROM role_permissions WHERE organization_id = ?";
        Result<Record> result = dsl.fetch(sql, organizationId);
        return result.into(RolePermission.class);
    }

    /**
     * 根据机构ID和角色获取权限
     */
    public Optional<RolePermission> findByOrganizationIdAndRole(Long organizationId, String role) {
        String sql = "SELECT * FROM role_permissions WHERE organization_id = ? AND role = ?";
        Result<Record> result = dsl.fetch(sql, organizationId, role);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0).into(RolePermission.class));
    }

    /**
     * 创建或更新角色权限
     */
    public RolePermission save(RolePermission permission) {
        if (permission.getId() == null) {
            String insertSql = "INSERT INTO role_permissions (organization_id, role, menu_permissions, action_permissions) " +
                    "VALUES (?, ?, ?, ?)";
            dsl.execute(insertSql, 
                    permission.getOrganizationId(), 
                    permission.getRole(),
                    permission.getMenuPermissions(), 
                    permission.getActionPermissions());
            
            String selectSql = "SELECT * FROM role_permissions WHERE organization_id = ? AND role = ?";
            Result<Record> result = dsl.fetch(selectSql, permission.getOrganizationId(), permission.getRole());
            return result.get(0).into(RolePermission.class);
        } else {
            String updateSql = "UPDATE role_permissions SET menu_permissions = ?, action_permissions = ? WHERE id = ?";
            dsl.execute(updateSql, 
                    permission.getMenuPermissions(), 
                    permission.getActionPermissions(),
                    permission.getId());
            return permission;
        }
    }

    /**
     * 根据ID删除权限
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM role_permissions WHERE id = ?";
        dsl.execute(sql, id);
    }
}

