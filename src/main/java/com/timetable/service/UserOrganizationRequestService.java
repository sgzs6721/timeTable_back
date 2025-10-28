package com.timetable.service;

import com.timetable.dto.UserOrganizationRequestDTO;
import com.timetable.entity.Organization;
import com.timetable.entity.UserOrganizationRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.OrganizationRepository;
import com.timetable.repository.UserOrganizationRequestRepository;
import com.timetable.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户机构申请服务层
 */
@Service
public class UserOrganizationRequestService {

    private static final Logger logger = LoggerFactory.getLogger(UserOrganizationRequestService.class);

    @Autowired
    private UserOrganizationRequestRepository requestRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 创建机构申请
     */
    @Transactional
    public UserOrganizationRequestDTO createRequest(String wechatOpenid, String wechatUnionid, 
                                                     String wechatNickname, String wechatAvatar, 
                                                     Byte wechatSex, Long organizationId, String applyReason) {
        // 验证机构是否存在
        Organization organization = organizationRepository.findById(organizationId);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }

        // 检查是否已有待审批的申请
        if (requestRepository.existsByWechatOpenidAndStatus(wechatOpenid, "PENDING")) {
            throw new RuntimeException("您已有待审批的申请，请耐心等待");
        }

        // 创建申请记录
        UserOrganizationRequest request = new UserOrganizationRequest();
        request.setOrganizationId(organizationId);
        request.setWechatOpenid(wechatOpenid);
        request.setWechatUnionid(wechatUnionid);
        request.setWechatNickname(wechatNickname);
        request.setWechatAvatar(wechatAvatar);
        request.setWechatSex(wechatSex);
        request.setApplyReason(applyReason);
        request.setStatus("PENDING");

        UserOrganizationRequest savedRequest = requestRepository.save(request);
        
        logger.info("用户申请加入机构：wechatOpenid={}, organizationId={}", wechatOpenid, organizationId);
        
        return convertToDTO(savedRequest);
    }

    /**
     * 根据微信OpenID获取最新申请
     */
    public UserOrganizationRequestDTO getRequestByWechatOpenid(String wechatOpenid) {
        UserOrganizationRequest request = requestRepository.findByWechatOpenid(wechatOpenid);
        return request != null ? convertToDTO(request) : null;
    }

    /**
     * 获取机构的所有申请
     */
    public List<UserOrganizationRequestDTO> getRequestsByOrganizationId(Long organizationId) {
        List<UserOrganizationRequest> requests = requestRepository.findByOrganizationId(organizationId);
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取待审批的申请
     */
    public List<UserOrganizationRequestDTO> getPendingRequests() {
        List<UserOrganizationRequest> requests = requestRepository.findByStatus("PENDING");
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取某个机构的待审批申请
     */
    public List<UserOrganizationRequestDTO> getPendingRequestsByOrganizationId(Long organizationId) {
        List<UserOrganizationRequest> requests = requestRepository.findByOrganizationIdAndStatus(organizationId, "PENDING");
        return requests.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 审批申请（同意）
     */
    @Transactional
    public UserOrganizationRequestDTO approveRequest(Long requestId, Long approverId, 
                                                      String defaultRole, String defaultPosition) {
        UserOrganizationRequest request = requestRepository.findById(requestId);
        if (request == null) {
            throw new RuntimeException("申请不存在");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("该申请已被处理");
        }

        // 创建用户账号
        Users newUser = new Users();
        
        // 生成用户名（使用微信昵称或随机生成）
        String username = generateUniqueUsername(request.getWechatNickname(), request.getWechatOpenid());
        newUser.setUsername(username);
        
        // 设置默认密码（可以是随机密码，用户可以通过微信登录后修改）
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        newUser.setPasswordHash(passwordEncoder.encode(randomPassword));
        
        // 设置角色和职位
        newUser.setRole(defaultRole != null ? defaultRole : "USER");
        newUser.setPosition(defaultPosition != null ? defaultPosition : "COACH");
        
        // 设置微信信息
        newUser.setWechatOpenid(request.getWechatOpenid());
        newUser.setWechatUnionid(request.getWechatUnionid());
        newUser.setWechatAvatar(request.getWechatAvatar());
        newUser.setWechatSex(request.getWechatSex());
        newUser.setNickname(request.getWechatNickname());
        
        // 设置机构ID
        newUser.setOrganizationId(request.getOrganizationId());
        
        // 设置状态
        newUser.setStatus("ACTIVE");
        newUser.setIsDeleted(false);
        
        // 保存用户
        Users savedUser = userRepository.createUser(newUser);
        
        // 更新申请状态
        request.setUserId(savedUser.getId());
        request.setStatus("APPROVED");
        request.setApprovedBy(approverId);
        request.setApprovedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        
        UserOrganizationRequest updatedRequest = requestRepository.update(request);
        
        logger.info("申请已批准：requestId={}, userId={}, organizationId={}", 
                    requestId, savedUser.getId(), request.getOrganizationId());
        
        return convertToDTO(updatedRequest);
    }

    /**
     * 审批申请（拒绝）
     */
    @Transactional
    public UserOrganizationRequestDTO rejectRequest(Long requestId, Long approverId, String rejectReason) {
        UserOrganizationRequest request = requestRepository.findById(requestId);
        if (request == null) {
            throw new RuntimeException("申请不存在");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("该申请已被处理");
        }

        // 更新申请状态
        request.setStatus("REJECTED");
        request.setApprovedBy(approverId);
        request.setApprovedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        request.setRejectReason(rejectReason);
        
        UserOrganizationRequest updatedRequest = requestRepository.update(request);
        
        logger.info("申请已拒绝：requestId={}, reason={}", requestId, rejectReason);
        
        return convertToDTO(updatedRequest);
    }

    /**
     * 生成唯一用户名
     */
    private String generateUniqueUsername(String wechatNickname, String wechatOpenid) {
        String baseUsername;
        
        if (wechatNickname != null && !wechatNickname.trim().isEmpty()) {
            // 移除特殊字符，只保留字母、数字和中文
            baseUsername = wechatNickname.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
            if (baseUsername.length() > 20) {
                baseUsername = baseUsername.substring(0, 20);
            }
        } else {
            // 使用openid的一部分
            baseUsername = "user_" + wechatOpenid.substring(0, Math.min(8, wechatOpenid.length()));
        }

        // 检查用户名是否已存在
        String username = baseUsername;
        int counter = 1;
        while (userRepository.findByUsername(username) != null) {
            username = baseUsername + "_" + counter;
            counter++;
        }

        return username;
    }

    /**
     * 转换为DTO
     */
    private UserOrganizationRequestDTO convertToDTO(UserOrganizationRequest request) {
        UserOrganizationRequestDTO dto = new UserOrganizationRequestDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUserId());
        dto.setOrganizationId(request.getOrganizationId());
        dto.setWechatOpenid(request.getWechatOpenid());
        dto.setWechatNickname(request.getWechatNickname());
        dto.setWechatAvatar(request.getWechatAvatar());
        dto.setWechatSex(request.getWechatSex());
        dto.setApplyReason(request.getApplyReason());
        dto.setStatus(request.getStatus());
        dto.setApprovedBy(request.getApprovedBy());
        dto.setApprovedAt(request.getApprovedAt());
        dto.setRejectReason(request.getRejectReason());
        dto.setCreatedAt(request.getCreatedAt());
        
        // 设置机构信息
        if (request.getOrganization() != null) {
            dto.setOrganizationName(request.getOrganization().getName());
            dto.setOrganizationAddress(request.getOrganization().getAddress());
        }
        
        // 设置审批人信息
        dto.setApprovedByUsername(request.getApprovedByUsername());
        
        return dto;
    }
}

