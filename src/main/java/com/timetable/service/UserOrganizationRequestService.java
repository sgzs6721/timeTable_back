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
                                                     Byte wechatSex, Long organizationId, String applyReason,
                                                     String wechatProvince, String wechatCity, String wechatCountry) {
        // 验证机构是否存在
        Organization organization = organizationRepository.findById(organizationId);
        if (organization == null) {
            throw new RuntimeException("机构不存在");
        }

        // 检查该用户在该机构是否已存在记录
        Users existingUser = userRepository.findByWechatOpenidAndOrganizationId(wechatOpenid, organizationId);
        if (existingUser != null) {
            // 该机构已有用户记录，检查状态
            if ("PENDING".equals(existingUser.getStatus())) {
                throw new RuntimeException("您在该机构已有待审批的申请，请耐心等待");
            } else if ("APPROVED".equals(existingUser.getStatus())) {
                throw new RuntimeException("您已是该机构成员，无需重复申请");
            } else if ("REJECTED".equals(existingUser.getStatus())) {
                // 如果之前被拒绝，允许重新申请，更新状态为PENDING
                existingUser.setStatus("PENDING");
                existingUser.setUpdatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
                userRepository.update(existingUser);
            }
        } else {
            // 创建PENDING状态的用户
            Users newUser = new Users();
            String username = generateUniqueUsername(wechatNickname, wechatOpenid);
            newUser.setUsername(username);
            newUser.setPasswordHash("");  // 微信用户暂不设置密码
            newUser.setRole("USER");
            newUser.setPosition("COACH");
            newUser.setNickname(wechatNickname);
            newUser.setStatus("PENDING");
            newUser.setOrganizationId(organizationId);
            
            newUser.setWechatOpenid(wechatOpenid);
            newUser.setWechatUnionid(wechatUnionid);
            newUser.setWechatAvatar(wechatAvatar);
            newUser.setWechatSex(wechatSex);
            newUser.setWechatProvince(wechatProvince);
            newUser.setWechatCity(wechatCity);
            newUser.setWechatCountry(wechatCountry);
            
            newUser.setIsDeleted((byte) 0);
            newUser.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
            newUser.setUpdatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
            
            userRepository.save(newUser);
            existingUser = userRepository.findByWechatOpenidAndOrganizationId(wechatOpenid, organizationId);
            
            logger.info("创建待审批用户：userId={}, wechatOpenid={}, organizationId={}", 
                existingUser.getId(), wechatOpenid, organizationId);
        }

        // 检查该机构是否已有待审批的申请记录
        if (requestRepository.existsByWechatOpenidAndOrganizationIdAndStatus(wechatOpenid, organizationId, "PENDING")) {
            throw new RuntimeException("您已有待审批的申请，请耐心等待");
        }

        // 创建申请记录
        UserOrganizationRequest request = new UserOrganizationRequest();
        request.setOrganizationId(organizationId);
        request.setUserId(existingUser.getId());
        request.setWechatOpenid(wechatOpenid);
        request.setWechatUnionid(wechatUnionid);
        request.setWechatNickname(wechatNickname);
        request.setWechatAvatar(wechatAvatar);
        request.setWechatSex(wechatSex);
        request.setApplyReason(applyReason);
        request.setStatus("PENDING");

        UserOrganizationRequest savedRequest = requestRepository.save(request);
        
        logger.info("用户申请加入机构：wechatOpenid={}, organizationId={}, userId={}", 
            wechatOpenid, organizationId, existingUser.getId());
        
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

        // 获取待审批的用户
        Users user = userRepository.findById(request.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户状态为APPROVED
        user.setStatus("APPROVED");
        if (defaultRole != null) {
            user.setRole(defaultRole);
        }
        if (defaultPosition != null) {
            user.setPosition(defaultPosition);
        }
        user.setUpdatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        userRepository.update(user);
        
        // 更新申请状态
        request.setStatus("APPROVED");
        request.setApprovedBy(approverId);
        request.setApprovedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        
        UserOrganizationRequest updatedRequest = requestRepository.update(request);
        
        logger.info("申请已批准：requestId={}, userId={}, organizationId={}", 
                    requestId, user.getId(), request.getOrganizationId());
        
        // 发送微信推送通知
        try {
            sendApprovalNotification(user, request);
        } catch (Exception e) {
            logger.error("发送审批通过通知失败：userId={}", user.getId(), e);
            // 不影响主流程，继续执行
        }
        
        return convertToDTO(updatedRequest);
    }
    
    /**
     * 发送审批通过通知
     */
    private void sendApprovalNotification(Users user, UserOrganizationRequest request) {
        // TODO: 实现微信推送通知
        // 这里需要调用微信模板消息接口
        logger.info("发送审批通过通知：userId={}, wechatOpenid={}", user.getId(), user.getWechatOpenid());
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

        // 更新用户状态为REJECTED
        Users user = userRepository.findById(request.getUserId());
        if (user != null) {
            user.setStatus("REJECTED");
            user.setUpdatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
            userRepository.update(user);
        }

        // 更新申请状态
        request.setStatus("REJECTED");
        request.setApprovedBy(approverId);
        request.setApprovedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        request.setRejectReason(rejectReason);
        
        UserOrganizationRequest updatedRequest = requestRepository.update(request);
        
        logger.info("申请已拒绝：requestId={}, userId={}, reason={}", requestId, request.getUserId(), rejectReason);
        
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

