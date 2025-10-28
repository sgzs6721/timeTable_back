package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.OrganizationDTO;
import com.timetable.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

