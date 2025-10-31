package com.timetable.service;

import com.timetable.dto.CustomerStatusChangeRequest;
import com.timetable.dto.CustomerStatusHistoryDTO;
import com.timetable.entity.Customer;
import com.timetable.entity.CustomerStatusHistory;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.CustomerRepository;
import com.timetable.repository.CustomerStatusHistoryRepository;
import com.timetable.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerStatusHistoryService {

    @Autowired
    private CustomerStatusHistoryRepository historyRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private WeeklyInstanceService weeklyInstanceService;

    @Transactional
    public CustomerStatusHistoryDTO changeStatus(Long customerId, CustomerStatusChangeRequest request, Long currentUserId) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 如果assignedSalesId为null，设置为当前用户
        if (customer.getAssignedSalesId() == null) {
            customer.setAssignedSalesId(currentUserId);
        }

        // 检查权限
        Users user = userRepository.findById(currentUserId);
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
        if (!isAdmin && !currentUserId.equals(customer.getAssignedSalesId())) {
            throw new RuntimeException("无权限修改此客户状态");
        }

        String fromStatus = customer.getStatus();
        String toStatus = request.getToStatus();

        // 更新客户状态（即使状态相同也允许记录，可能是添加备注或体验课信息）
        customer.setStatus(toStatus);
        customerRepository.update(customer);

        // 记录状态变更
        CustomerStatusHistory history = new CustomerStatusHistory();
        history.setCustomerId(customerId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setNotes(request.getNotes());
        history.setCreatedBy(currentUserId);
        
        // 保存体验课程时间（如果有）
        if (request.getTrialScheduleDate() != null && !request.getTrialScheduleDate().isEmpty()) {
            history.setTrialScheduleDate(java.time.LocalDate.parse(request.getTrialScheduleDate()));
        }
        if (request.getTrialStartTime() != null && !request.getTrialStartTime().isEmpty()) {
            history.setTrialStartTime(java.time.LocalTime.parse(request.getTrialStartTime()));
        }
        if (request.getTrialEndTime() != null && !request.getTrialEndTime().isEmpty()) {
            history.setTrialEndTime(java.time.LocalTime.parse(request.getTrialEndTime()));
        }
        history.setTrialCoachId(request.getTrialCoachId());

        CustomerStatusHistory savedHistory = historyRepository.save(history);
        return convertToDTO(savedHistory);
    }

    public List<CustomerStatusHistoryDTO> getHistoryByCustomerId(Long customerId, Long currentUserId) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 检查权限
        Users user = userRepository.findById(currentUserId);
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
        // 如果assignedSalesId为null，允许当前用户查看（历史遗留数据）
        if (!isAdmin && customer.getAssignedSalesId() != null && !currentUserId.equals(customer.getAssignedSalesId())) {
            throw new RuntimeException("无权限查看此客户记录");
        }

        List<CustomerStatusHistory> histories = historyRepository.findByCustomerId(customerId);
        return histories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerStatusHistoryDTO updateHistory(Long historyId, String notes, Long currentUserId) {
        CustomerStatusHistory history = historyRepository.findById(historyId);
        if (history == null) {
            throw new RuntimeException("历史记录不存在");
        }

        // 检查权限 - 只能修改自己创建的记录或管理员可以修改
        Users user = userRepository.findById(currentUserId);
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
        if (!isAdmin && !currentUserId.equals(history.getCreatedBy())) {
            throw new RuntimeException("无权限修改此历史记录");
        }

        // 更新备注
        history.setNotes(notes);
        CustomerStatusHistory updatedHistory = historyRepository.update(history);
        return convertToDTO(updatedHistory);
    }

    @Transactional
    public void deleteHistory(Long historyId, Long currentUserId) {
        CustomerStatusHistory history = historyRepository.findById(historyId);
        if (history == null) {
            throw new RuntimeException("历史记录不存在");
        }

        // 检查权限 - 只能删除自己创建的记录或管理员可以删除
        Users user = userRepository.findById(currentUserId);
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
        if (!isAdmin && !currentUserId.equals(history.getCreatedBy())) {
            throw new RuntimeException("无权限删除此历史记录");
        }

        // 获取客户信息
        Customer customer = customerRepository.findById(history.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 如果删除的是当前状态的流转记录，需要将客户状态回退到前一个状态
        if (customer.getStatus() != null && customer.getStatus().equals(history.getToStatus())) {
            // 回退到 fromStatus
            customer.setStatus(history.getFromStatus());
            customerRepository.update(customer);
        }

        // 删除历史记录
        historyRepository.delete(historyId);
    }

    private CustomerStatusHistoryDTO convertToDTO(CustomerStatusHistory history) {
        CustomerStatusHistoryDTO dto = new CustomerStatusHistoryDTO();
        dto.setId(history.getId());
        dto.setCustomerId(history.getCustomerId());
        dto.setFromStatus(history.getFromStatus());
        dto.setFromStatusText(getStatusText(history.getFromStatus()));
        dto.setToStatus(history.getToStatus());
        dto.setToStatusText(getStatusText(history.getToStatus()));
        
        // 获取notes：对于新建状态（fromStatus为null或toStatus为NEW），如果notes为空，从客户表获取
        String notes = history.getNotes();
        boolean isNewStatus = (history.getFromStatus() == null || history.getFromStatus().isEmpty() || "NEW".equals(history.getToStatus()));
        
        if (isNewStatus && (notes == null || notes.trim().isEmpty())) {
            Customer customer = customerRepository.findById(history.getCustomerId());
            if (customer != null && customer.getNotes() != null && !customer.getNotes().trim().isEmpty()) {
                notes = customer.getNotes();
            }
        }
        dto.setNotes(notes);
        
        dto.setCreatedBy(history.getCreatedBy());
        dto.setCreatedAt(history.getCreatedAt());

        // 获取操作人姓名
        Users creator = userRepository.findById(history.getCreatedBy());
        if (creator != null) {
            dto.setCreatedByName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }
        
        // 设置体验课程相关信息
        dto.setTrialScheduleDate(history.getTrialScheduleDate());
        dto.setTrialStartTime(history.getTrialStartTime());
        dto.setTrialEndTime(history.getTrialEndTime());
        dto.setTrialCoachId(history.getTrialCoachId());
        dto.setTrialCancelled(history.getTrialCancelled());

        return dto;
    }

    private String getStatusText(String status) {
        if (status == null) {
            return "无";
        }
        switch (status) {
            case "NEW": return "新建";
            case "CONTACTED": return "已联系";
            case "SCHEDULED": return "待体验";
            case "PENDING_CONFIRM": return "待确认";
            case "VISITED": return "已体验";
            case "RE_EXPERIENCE": return "待再体验";
            case "PENDING_SOLD": return "待成交";
            case "SOLD": return "已成交";
            case "CLOSED": return "已结束";
            default: return status;
        }
    }
    
    /**
     * 标记体验课程为已取消（仅标记，不删除课表）
     */
    public boolean markTrialAsCancelled(Long historyId) {
        try {
            return historyRepository.markTrialAsCancelled(historyId);
        } catch (Exception e) {
            throw new RuntimeException("标记体验课程取消失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 取消体验课程（事务：标记取消 + 删除课表）
     * 直接从历史记录读取课表ID，不需要外部传参
     * @param historyId 历史记录ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTrialScheduleWithTransaction(Long historyId) {
        
        // 1. 获取历史记录，读取课表ID
        CustomerStatusHistory history = historyRepository.findById(historyId);
        if (history == null) {
            throw new RuntimeException("未找到该历史记录");
        }
        
        // 2. 标记历史记录为已取消
        boolean marked = historyRepository.markTrialAsCancelled(historyId);
        if (!marked) {
            throw new RuntimeException("标记体验课程取消失败");
        }
        
        // 3. 如果历史记录中存储了课表信息，删除课表中的课程
        Long trialScheduleId = history.getTrialScheduleId();
        Long trialTimetableId = history.getTrialTimetableId();
        String sourceType = history.getTrialSourceType();
        
        if (trialScheduleId != null && sourceType != null) {
            try {
                if ("weekly_instance".equals(sourceType)) {
                    // 删除周实例课程
                    weeklyInstanceService.deleteInstanceSchedule(trialScheduleId);
                } else if ("schedule".equals(sourceType)) {
                    // 删除普通课程，需要 timetableId
                    if (trialTimetableId == null) {
                        throw new RuntimeException("删除普通课程时缺少 timetableId");
                    }
                    boolean deleted = scheduleService.deleteSchedule(trialTimetableId, trialScheduleId);
                    if (!deleted) {
                        throw new RuntimeException("删除课表中的课程失败");
                    }
                }
            } catch (Exception e) {
                // 删除课表失败，抛出异常触发事务回滚
                throw new RuntimeException("删除课表中的体验课程失败: " + e.getMessage(), e);
            }
        }
        
        return true;
    }
}

