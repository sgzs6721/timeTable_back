package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.ApplyOrganizationRequest;
import com.timetable.dto.ApproveOrganizationRequestDTO;
import com.timetable.dto.UserOrganizationRequestDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.UserRepository;
import com.timetable.service.UserOrganizationRequestService;
import com.timetable.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 用户机构申请控制器
 */
@RestController
@RequestMapping("/organization-requests")
@Validated
public class UserOrganizationRequestController {

    private static final Logger logger = LoggerFactory.getLogger(UserOrganizationRequestController.class);

    @Autowired
    private UserOrganizationRequestService requestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前用户的申请状态（通过token）
     */
    @GetMapping("/my-request")
    public ResponseEntity<ApiResponse<UserOrganizationRequestDTO>> getMyRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("未提供认证信息"));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            Users user = userRepository.findByUsername(username);
            if (user == null || user.getWechatOpenid() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户信息异常"));
            }

            UserOrganizationRequestDTO requestDTO = requestService.getRequestByWechatOpenid(user.getWechatOpenid());
            if (requestDTO == null) {
                return ResponseEntity.ok(ApiResponse.success("暂无申请记录", null));
            }

            return ResponseEntity.ok(ApiResponse.success("获取申请信息成功", requestDTO));

        } catch (Exception e) {
            logger.error("获取申请信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取申请信息失败"));
        }
    }

    /**
     * 获取所有待审批的申请（管理员接口）
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<UserOrganizationRequestDTO>>> getPendingRequests() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Users currentUser = userRepository.findByUsername(username);

            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }

            List<UserOrganizationRequestDTO> requests;
            
            // 如果是管理员，可以看到所有申请
            if ("ADMIN".equals(currentUser.getRole())) {
                requests = requestService.getPendingRequests();
            } else {
                // 普通用户只能看到自己机构的申请（如果有权限的话）
                if (currentUser.getOrganizationId() == null) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("无权限查看申请"));
                }
                requests = requestService.getPendingRequestsByOrganizationId(currentUser.getOrganizationId());
            }

            return ResponseEntity.ok(ApiResponse.success("获取待审批申请成功", requests));

        } catch (Exception e) {
            logger.error("获取待审批申请失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取待审批申请失败"));
        }
    }

    /**
     * 审批申请
     */
    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<UserOrganizationRequestDTO>> approveRequest(
            @Valid @RequestBody ApproveOrganizationRequestDTO approveDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Users currentUser = userRepository.findByUsername(username);

            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }

            // 检查是否是管理员
            if (!"ADMIN".equals(currentUser.getRole())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限审批申请"));
            }

            UserOrganizationRequestDTO result;
            
            if (approveDTO.getApproved()) {
                // 同意申请
                result = requestService.approveRequest(
                    approveDTO.getRequestId(),
                    currentUser.getId(),
                    approveDTO.getDefaultRole(),
                    approveDTO.getDefaultPosition()
                );
                logger.info("管理员 {} 同意了申请 {}", username, approveDTO.getRequestId());
                return ResponseEntity.ok(ApiResponse.success("申请已批准", result));
            } else {
                // 拒绝申请
                if (approveDTO.getRejectReason() == null || approveDTO.getRejectReason().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("拒绝申请时必须提供拒绝理由"));
                }
                result = requestService.rejectRequest(
                    approveDTO.getRequestId(),
                    currentUser.getId(),
                    approveDTO.getRejectReason()
                );
                logger.info("管理员 {} 拒绝了申请 {}", username, approveDTO.getRequestId());
                return ResponseEntity.ok(ApiResponse.success("申请已拒绝", result));
            }

        } catch (RuntimeException e) {
            logger.warn("审批申请失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审批申请失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("审批申请失败"));
        }
    }

    /**
     * 获取某个机构的所有申请（管理员接口）
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<UserOrganizationRequestDTO>>> getRequestsByOrganization(
            @PathVariable Long organizationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Users currentUser = userRepository.findByUsername(username);

            if (currentUser == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }

            // 检查权限：管理员或者该机构的用户
            if (!"ADMIN".equals(currentUser.getRole()) && 
                !organizationId.equals(currentUser.getOrganizationId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无权限查看该机构的申请"));
            }

            List<UserOrganizationRequestDTO> requests = requestService.getRequestsByOrganizationId(organizationId);
            return ResponseEntity.ok(ApiResponse.success("获取申请列表成功", requests));

        } catch (Exception e) {
            logger.error("获取申请列表失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取申请列表失败"));
        }
    }
}

