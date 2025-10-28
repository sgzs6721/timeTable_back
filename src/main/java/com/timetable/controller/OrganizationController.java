package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.OrganizationDTO;
import com.timetable.entity.Organization;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserRepository;
import com.timetable.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 机构控制器
 */
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取所有活跃机构（公开接口，用于微信登录时选择机构）
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<OrganizationDTO>>> getActiveOrganizations() {
        try {
            List<OrganizationDTO> organizations = organizationService.getActiveOrganizations();
            return ResponseEntity.ok(ApiResponse.success("获取机构列表成功", organizations));
        } catch (Exception e) {
            logger.error("获取活跃机构列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取机构列表失败"));
        }
    }

    /**
     * 获取所有机构（管理员接口）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationDTO>>> getAllOrganizations() {
        try {
            List<OrganizationDTO> organizations = organizationService.getAllOrganizations();
            return ResponseEntity.ok(ApiResponse.success("获取机构列表成功", organizations));
        } catch (Exception e) {
            logger.error("获取机构列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取机构列表失败"));
        }
    }

    /**
     * 根据ID获取机构
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDTO>> getOrganizationById(@PathVariable Long id) {
        try {
            OrganizationDTO organization = organizationService.getOrganizationById(id);
            return ResponseEntity.ok(ApiResponse.success("获取机构信息成功", organization));
        } catch (RuntimeException e) {
            logger.warn("获取机构信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取机构信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取机构信息失败"));
        }
    }

    /**
     * 创建机构（管理员接口）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationDTO>> createOrganization(
            @Valid @RequestBody Map<String, String> organizationData) {
        try {
            // 检查权限
            if (!isAdmin()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限创建机构"));
            }

            Organization organization = new Organization();
            organization.setName(organizationData.get("name"));
            organization.setCode(organizationData.get("code"));
            organization.setAddress(organizationData.get("address"));
            organization.setContactPhone(organizationData.get("contactPhone"));
            organization.setContactPerson(organizationData.get("contactPerson"));
            organization.setStatus("ACTIVE");

            OrganizationDTO result = organizationService.createOrganization(organization);
            logger.info("创建机构成功: {}", result.getName());
            return ResponseEntity.ok(ApiResponse.success("创建机构成功", result));

        } catch (RuntimeException e) {
            logger.warn("创建机构失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建机构失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("创建机构失败"));
        }
    }

    /**
     * 更新机构（管理员接口）
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDTO>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, String> organizationData) {
        try {
            // 检查权限
            if (!isAdmin()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限更新机构"));
            }

            Organization organization = new Organization();
            organization.setName(organizationData.get("name"));
            organization.setCode(organizationData.get("code"));
            organization.setAddress(organizationData.get("address"));
            organization.setContactPhone(organizationData.get("contactPhone"));
            organization.setContactPerson(organizationData.get("contactPerson"));
            organization.setStatus(organizationData.getOrDefault("status", "ACTIVE"));

            OrganizationDTO result = organizationService.updateOrganization(id, organization);
            logger.info("更新机构成功: {}", result.getName());
            return ResponseEntity.ok(ApiResponse.success("更新机构成功", result));

        } catch (RuntimeException e) {
            logger.warn("更新机构失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新机构失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("更新机构失败"));
        }
    }

    /**
     * 删除机构（管理员接口）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Long id) {
        try {
            // 检查权限
            if (!isAdmin()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限删除机构"));
            }

            organizationService.deleteOrganization(id);
            logger.info("删除机构成功: {}", id);
            return ResponseEntity.ok(ApiResponse.success("删除机构成功"));

        } catch (RuntimeException e) {
            logger.warn("删除机构失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除机构失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("删除机构失败"));
        }
    }

    /**
     * 获取机构的管理员列表
     */
    @GetMapping("/{id}/admins")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrganizationAdmins(
            @PathVariable Long id) {
        try {
            // 检查权限
            if (!isAdmin()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限查看机构管理员"));
            }

            List<Map<String, Object>> admins = organizationService.getOrganizationAdmins(id);
            return ResponseEntity.ok(ApiResponse.success("获取机构管理员成功", admins));

        } catch (Exception e) {
            logger.error("获取机构管理员失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取机构管理员失败"));
        }
    }

    /**
     * 设置用户为机构管理员
     */
    @PostMapping("/{id}/admins/{userId}")
    public ResponseEntity<ApiResponse<Void>> setOrganizationAdmin(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            // 检查权限
            if (!isAdmin()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限设置机构管理员"));
            }

            organizationService.setOrganizationAdmin(id, userId);
            logger.info("设置机构管理员成功: organizationId={}, userId={}", id, userId);
            return ResponseEntity.ok(ApiResponse.success("设置机构管理员成功"));

        } catch (RuntimeException e) {
            logger.warn("设置机构管理员失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("设置机构管理员失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("设置机构管理员失败"));
        }
    }

    /**
     * 移除机构管理员
     */
    @DeleteMapping("/{id}/admins/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeOrganizationAdmin(
            @PathVariable Long id,
            @PathVariable Long userId) {
        try {
            // 检查权限
            if (!isAdmin()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限移除机构管理员"));
            }

            organizationService.removeOrganizationAdmin(id, userId);
            logger.info("移除机构管理员成功: organizationId={}, userId={}", id, userId);
            return ResponseEntity.ok(ApiResponse.success("移除机构管理员成功"));

        } catch (RuntimeException e) {
            logger.warn("移除机构管理员失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("移除机构管理员失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("移除机构管理员失败"));
        }
    }

    /**
     * 检查当前用户是否是管理员
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        Users user = userRepository.findByUsername(username);
        return user != null && "ADMIN".equals(user.getRole());
    }
}

