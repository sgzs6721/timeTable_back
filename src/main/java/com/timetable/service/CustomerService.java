package com.timetable.service;

import com.timetable.dto.CustomerDTO;
import com.timetable.dto.CustomerRequest;
import com.timetable.dto.TrialCustomerDTO;
import com.timetable.entity.Customer;
import com.timetable.entity.CustomerStatusHistory;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.CustomerRepository;
import com.timetable.repository.CustomerStatusHistoryRepository;
import com.timetable.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public CustomerDTO createCustomer(CustomerRequest request, Long currentUserId) {
        Customer customer = new Customer();
        customer.setChildName(request.getChildName());
        customer.setParentPhone(request.getParentPhone());
        customer.setStatus(request.getStatus() != null ? request.getStatus() : "NEW");
        customer.setNotes(request.getNotes());
        customer.setSource(request.getSource());
        customer.setAssignedSalesId(currentUserId);
        customer.setCreatedBy(currentUserId);
        // 设置 organizationId
        Users currentUser = userRepository.findById(currentUserId);
        if (currentUser != null) {
            customer.setOrganizationId(currentUser.getOrganizationId());
        } else {
            customer.setOrganizationId(null);
        }
        Customer savedCustomer = customerRepository.save(customer);
        
        // 自动创建状态流转记录（新建客户）
        if (savedCustomer.getId() != null) {
            System.out.println("=== 创建客户状态流转记录 ===");
            System.out.println("客户ID: " + savedCustomer.getId());
            System.out.println("客户姓名: " + savedCustomer.getChildName());
            System.out.println("备注信息: " + request.getNotes());
            System.out.println("状态: " + savedCustomer.getStatus());
            
            CustomerStatusHistory initialHistory = new CustomerStatusHistory();
            initialHistory.setCustomerId(savedCustomer.getId());
            initialHistory.setFromStatus(null); // 新建客户，从无到有
            initialHistory.setToStatus(savedCustomer.getStatus());
            initialHistory.setNotes(request.getNotes()); // 保存客户的备注信息到流转记录
            initialHistory.setCreatedBy(currentUserId);
            initialHistory.setCreatedAt(LocalDateTime.now());
            
            CustomerStatusHistory savedHistory = statusHistoryRepository.save(initialHistory);
            System.out.println("流转记录已创建，ID: " + savedHistory.getId());
            System.out.println("流转记录备注: " + savedHistory.getNotes());
        }
        
        return convertToDTO(savedCustomer);
    }

    @Transactional
    public CustomerDTO updateCustomer(Long customerId, CustomerRequest request, Long currentUserId) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 如果assignedSalesId为null，设置为当前用户
        if (customer.getAssignedSalesId() == null) {
            customer.setAssignedSalesId(currentUserId);
        }

        // 检查权限：只有管理员或分配的销售可以修改
        if (!currentUserId.equals(customer.getAssignedSalesId()) && 
            !isAdmin(currentUserId)) {
            throw new RuntimeException("无权限修改此客户");
        }

        customer.setChildName(request.getChildName());
        customer.setParentPhone(request.getParentPhone());
        customer.setStatus(request.getStatus());
        customer.setNotes(request.getNotes());
        customer.setSource(request.getSource());
        // 不需要 currentUser，直接不赋值 organizationId，客户归属机构只允许新建时指定
        // customer.setOrganizationId(XXX) 这一行删掉，update 仅修改官方字段

        Customer updatedCustomer = customerRepository.update(customer);
        return convertToDTO(updatedCustomer);
    }

    public List<CustomerDTO> getCustomersForUser(Long userId, boolean isAdmin) {
        List<Customer> customers;
        if (isAdmin) {
            customers = customerRepository.findAll();
        } else {
            customers = customerRepository.findByAssignedSalesId(userId);
        }
        
        return customers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomerById(Long customerId, Long currentUserId, boolean isAdmin) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 检查权限
        if (!isAdmin && !currentUserId.equals(customer.getAssignedSalesId())) {
            throw new RuntimeException("无权限查看此客户");
        }

        return convertToDTO(customer);
    }

    @Transactional
    public void deleteCustomer(Long customerId, Long currentUserId, boolean isAdmin) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 检查权限
        if (!isAdmin && !currentUserId.equals(customer.getAssignedSalesId())) {
            throw new RuntimeException("无权限删除此客户");
        }

        customerRepository.deleteById(customerId);
    }

    public List<CustomerDTO> getCustomersByStatus(String status, Long userId, boolean isAdmin) {
        List<Customer> customers;
        if (isAdmin) {
            customers = customerRepository.findByStatus(status);
        } else {
            List<Customer> allCustomers = customerRepository.findByAssignedSalesId(userId);
            customers = allCustomers.stream()
                    .filter(c -> status.equals(c.getStatus()))
                    .collect(Collectors.toList());
        }
        
        return customers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Customer findByChildName(String childName) {
        return customerRepository.findByChildName(childName);
    }

    public Customer findByChildNameLike(String childName) {
        return customerRepository.findByChildNameLike(childName);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id);
    }

    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setChildName(customer.getChildName());
        dto.setChildGender(customer.getChildGender());
        dto.setChildAge(customer.getChildAge());
        dto.setGrade(customer.getGrade());
        dto.setParentPhone(customer.getParentPhone());
        dto.setWechat(customer.getWechat());
        dto.setParentRelation(customer.getParentRelation());
        dto.setAvailableTime(customer.getAvailableTime());
        dto.setSource(customer.getSource());
        dto.setStatus(customer.getStatus());
        dto.setStatusText(getStatusText(customer.getStatus()));
        dto.setNotes(customer.getNotes());
        dto.setNextContactTime(customer.getNextContactTime());
        dto.setVisitTime(customer.getVisitTime());
        dto.setAssignedSalesId(customer.getAssignedSalesId());
        dto.setCreatedBy(customer.getCreatedBy());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        dto.setOrganizationId(customer.getOrganizationId()); // 新增

        // 获取销售姓名
        if (customer.getAssignedSalesId() != null) {
            Users sales = userRepository.findById(customer.getAssignedSalesId());
            if (sales != null) {
                dto.setAssignedSalesName(sales.getNickname() != null ? sales.getNickname() : sales.getUsername());
            }
        }

        // 获取创建者姓名
        Users creator = userRepository.findById(customer.getCreatedBy());
        if (creator != null) {
            dto.setCreatedByName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }

        // 获取最后一次状态流转记录
        List<CustomerStatusHistory> histories = statusHistoryRepository.findByCustomerId(customer.getId());
        if (histories != null && !histories.isEmpty()) {
            CustomerStatusHistory lastHistory = histories.get(0); // 已按时间倒序排列
            dto.setLastStatusChangeNote(lastHistory.getNotes());
            dto.setLastStatusChangeTime(lastHistory.getCreatedAt());
        }

        return dto;
    }

    private String getStatusText(String status) {
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

    private boolean isAdmin(Long userId) {
        Users user = userRepository.findById(userId);
        return user != null && "ADMIN".equals(user.getRole());
    }

    public List<TrialCustomerDTO> getTrialCustomers(Long userId, Long organizationId) {
        List<TrialCustomerDTO> result = new ArrayList<>();
        
        // 获取用户所在机构的所有待体验和待再体验客户
        List<Customer> customers = customerRepository.findByOrganizationId(organizationId);
        
        for (Customer customer : customers) {
            if ("SCHEDULED".equals(customer.getStatus()) || "RE_EXPERIENCE".equals(customer.getStatus())) {
                // 获取最新的体验时间信息
                List<CustomerStatusHistory> histories = statusHistoryRepository.findByCustomerId(customer.getId());
                CustomerStatusHistory latestTrialHistory = null;
                int trialCount = 0;
                
                // 统计体验次数：记录中有多少条状态变为"待体验"或"待再体验"的记录
                for (CustomerStatusHistory history : histories) {
                    if ("SCHEDULED".equals(history.getToStatus()) || "RE_EXPERIENCE".equals(history.getToStatus())) {
                        trialCount++;
                        // 同时找最新的体验安排（第一条包含体验时间的记录）
                        if (latestTrialHistory == null && history.getTrialScheduleDate() != null) {
                            latestTrialHistory = history;
                        }
                    }
                }
                
                if (latestTrialHistory != null && latestTrialHistory.getTrialScheduleDate() != null) {
                    TrialCustomerDTO dto = new TrialCustomerDTO();
                    dto.setCustomerId(customer.getId());
                    dto.setChildName(customer.getChildName());
                    dto.setParentPhone(customer.getParentPhone());
                    dto.setStatus(customer.getStatus());
                    dto.setStatusText(getStatusText(customer.getStatus()));
                    dto.setTrialScheduleDate(latestTrialHistory.getTrialScheduleDate());
                    dto.setTrialStartTime(latestTrialHistory.getTrialStartTime());
                    dto.setTrialEndTime(latestTrialHistory.getTrialEndTime());
                    dto.setTrialCoachId(latestTrialHistory.getTrialCoachId());
                    dto.setTrialCount(trialCount);
                    
                    // 获取教练名称
                    if (latestTrialHistory.getTrialCoachId() != null) {
                        Users coach = userRepository.findById(latestTrialHistory.getTrialCoachId());
                        if (coach != null) {
                            dto.setTrialCoachName(coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
                        }
                    }
                    
                    result.add(dto);
                }
            }
        }
        
        // 按体验日期和时间排序
        result.sort((a, b) -> {
            int dateCompare = a.getTrialScheduleDate().compareTo(b.getTrialScheduleDate());
            if (dateCompare != 0) return dateCompare;
            if (a.getTrialStartTime() != null && b.getTrialStartTime() != null) {
                return a.getTrialStartTime().compareTo(b.getTrialStartTime());
            }
            return 0;
        });
        
        return result;
    }
}
