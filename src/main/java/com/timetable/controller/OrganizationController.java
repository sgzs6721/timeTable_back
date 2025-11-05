package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.OrgManagementAuthRequest;
import com.timetable.dto.OrganizationDTO;
import com.timetable.dto.UserOrganizationRequestDTO;
import com.timetable.entity.Organization;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserRepository;
import com.timetable.service.OrganizationService;
import com.timetable.service.UserOrganizationRequestService;
import com.timetable.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private UserService userService;

    @Autowired
    private UserOrganizationRequestService requestService;

    @Value("${organization.management.username}")
    private String orgMgmtUsername;

    @Value("${organization.management.password}")
    private String orgMgmtPassword;

    /**
     * 机构管理访问验证（独立的用户名密码，不依赖系统用户表）
     */
    @PostMapping("/auth/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOrgManagementAuth(
            @Valid @RequestBody OrgManagementAuthRequest request) {
        try {
            logger.info("机构管理访问验证请求: {}", request.getUsername());
            
            // 直接验证配置文件中的用户名密码
            if (!orgMgmtUsername.equals(request.getUsername())) {
                logger.warn("机构管理用户名错误: {}", request.getUsername());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名或密码错误"));
            }
            
            if (!orgMgmtPassword.equals(request.getPassword())) {
                logger.warn("机构管理密码错误");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户名或密码错误"));
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("verified", true);
            data.put("username", request.getUsername());
            
            logger.info("机构管理访问验证成功: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.success("验证成功", data));
            
        } catch (Exception e) {
            logger.error("机构管理访问验证失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("验证失败，请稍后重试"));
        }
    }

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
     * 已登录用户通过机构代码申请加入其他机构
     */
    @PostMapping("/apply-by-code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyToOrganizationByCode(
            @Valid @RequestBody Map<String, String> requestBody,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户未登录"));
            }

            String organizationCode = requestBody.get("organizationCode");
            if (organizationCode == null || organizationCode.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("机构代码不能为空"));
            }

            // 获取当前用户
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }

            // 查找机构
            Organization organization = organizationService.getOrganizationByCode(organizationCode.trim());
            if (organization == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("机构代码不存在，请确认后重试"));
            }

            // 检查用户是否已经在该机构
            if (user.getOrganizationId() != null && user.getOrganizationId().equals(organization.getId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("您已经是该机构的成员"));
            }

            // 创建申请（使用用户的昵称或用户名）
            String nickname = user.getNickname() != null ? user.getNickname() : user.getUsername();
            
            UserOrganizationRequestDTO result = requestService.createRequestForExistingUser(
                user.getId(),
                user.getWechatOpenid(),
                user.getWechatUnionid(),
                nickname,
                user.getWechatAvatar(),
                user.getWechatSex(),
                organization.getId(),
                "申请加入" + organization.getName(),
                user.getWechatProvince(),
                user.getWechatCity(),
                user.getWechatCountry()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("id", result.getId());
            data.put("organizationName", organization.getName());
            data.put("status", result.getStatus());
            data.put("createdAt", result.getCreatedAt());

            logger.info("用户 {} (ID: {}) 提交申请加入机构 {} (ID: {})", 
                user.getUsername(), user.getId(), organization.getName(), organization.getId());

            return ResponseEntity.ok(ApiResponse.success(
                "申请已提交至" + organization.getName() + "，请等待管理员审批", data));

        } catch (RuntimeException e) {
            logger.warn("申请加入机构失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("申请加入机构失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("提交失败，请稍后重试"));
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

