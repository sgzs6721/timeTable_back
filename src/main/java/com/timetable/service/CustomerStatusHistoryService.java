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

        // 如果状态没有变化，不需要记录
        if (fromStatus != null && fromStatus.equals(toStatus)) {
            throw new RuntimeException("新状态与当前状态相同");
        }

        // 更新客户状态
        customer.setStatus(toStatus);
        customerRepository.update(customer);

        // 记录状态变更
        CustomerStatusHistory history = new CustomerStatusHistory();
        history.setCustomerId(customerId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setNotes(request.getNotes());
        history.setCreatedBy(currentUserId);

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

    private CustomerStatusHistoryDTO convertToDTO(CustomerStatusHistory history) {
        CustomerStatusHistoryDTO dto = new CustomerStatusHistoryDTO();
        dto.setId(history.getId());
        dto.setCustomerId(history.getCustomerId());
        dto.setFromStatus(history.getFromStatus());
        dto.setFromStatusText(getStatusText(history.getFromStatus()));
        dto.setToStatus(history.getToStatus());
        dto.setToStatusText(getStatusText(history.getToStatus()));
        dto.setNotes(history.getNotes());
        dto.setCreatedBy(history.getCreatedBy());
        dto.setCreatedAt(history.getCreatedAt());

        // 获取操作人姓名
        Users creator = userRepository.findById(history.getCreatedBy());
        if (creator != null) {
            dto.setCreatedByName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

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
            case "SOLD": return "已成交";
            case "RE_EXPERIENCE": return "待再体验";
            case "CLOSED": return "已结束";
            default: return status;
        }
    }
}

