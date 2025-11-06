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

import java.time.LocalDate;
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

        // 检查权限：只有管理员、分配的销售或创建者可以修改
        if (!currentUserId.equals(customer.getAssignedSalesId()) && 
            !currentUserId.equals(customer.getCreatedBy()) &&
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

    public List<CustomerDTO> getCustomersForUser(Long userId, boolean isAdmin, int page, int pageSize) {
        List<Customer> customers;
        if (isAdmin) {
            customers = customerRepository.findAllWithPagination(page, pageSize);
        } else {
            customers = customerRepository.findByAssignedSalesIdWithPagination(userId, page, pageSize);
        }
        
        return customers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CustomerDTO> getCustomersWithFilters(Long userId, Long organizationId, boolean isAdmin, int page, int pageSize,
                                                       String status, Long salesId, LocalDate filterDate, String keyword) {
        List<Customer> customers;
        if (isAdmin) {
            customers = customerRepository.findAllWithFiltersAndPagination(organizationId, page, pageSize, status, salesId, filterDate, keyword);
        } else {
            customers = customerRepository.findByUserWithFiltersAndPagination(userId, page, pageSize, status, filterDate, keyword);
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

        // 检查权限：管理员、分配的销售或创建者可以查看
        if (!isAdmin && !currentUserId.equals(customer.getAssignedSalesId()) && !currentUserId.equals(customer.getCreatedBy())) {
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

        // 检查权限：管理员、分配的销售或创建者可以删除
        if (!isAdmin && !currentUserId.equals(customer.getAssignedSalesId()) && !currentUserId.equals(customer.getCreatedBy())) {
            throw new RuntimeException("无权限删除此客户");
        }

        customerRepository.deleteById(customerId);
    }

    public List<CustomerDTO> getCustomersByStatus(String status, Long userId, Long organizationId, boolean isAdmin) {
        List<Customer> customers;
        if (isAdmin) {
            customers = customerRepository.findByStatus(organizationId, status);
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

    public List<TrialCustomerDTO> getTrialCustomers(Long userId, Long organizationId, Long createdByIdFilter, LocalDate trialDateFilter, boolean includeAll) {
        List<TrialCustomerDTO> result = new ArrayList<>();
        
        // 获取用户所在机构的所有客户（不限状态）
        List<Customer> customers = customerRepository.findByOrganizationId(organizationId);
        
        for (Customer customer : customers) {
            // 获取该客户的所有状态流转历史
            List<CustomerStatusHistory> histories = statusHistoryRepository.findByCustomerId(customer.getId());
            
            // 遍历所有历史记录，查找所有有体验时间的记录
            for (CustomerStatusHistory history : histories) {
                // 检查是否为体验相关状态
                boolean isTrialStatus = ("SCHEDULED".equals(history.getToStatus()) || "RE_EXPERIENCE".equals(history.getToStatus()));
                
                if (isTrialStatus && history.getTrialScheduleDate() != null) {
                    // 如果不是includeAll模式，只显示当前状态仍为待体验或待再体验的记录（且未取消）
                    if (!includeAll) {
                        if (!("SCHEDULED".equals(customer.getStatus()) || "RE_EXPERIENCE".equals(customer.getStatus()))) {
                            continue;
                        }
                        // 非includeAll模式下，过滤掉已取消的体验记录
                        if (Boolean.TRUE.equals(history.getTrialCancelled())) {
                            continue;
                        }
                    }
                    // includeAll模式下，已取消的记录也会包含在结果中
                    
                    // 应用日期过滤
                    if (trialDateFilter != null && !history.getTrialScheduleDate().equals(trialDateFilter)) {
                        continue;
                    }
                    
                    // 应用创建人ID过滤
                    if (createdByIdFilter != null && !createdByIdFilter.equals(history.getCreatedBy())) {
                        continue;
                    }
                    
                    TrialCustomerDTO dto = new TrialCustomerDTO();
                    dto.setCustomerId(customer.getId());
                    dto.setChildName(customer.getChildName());
                    dto.setParentPhone(customer.getParentPhone());
                    dto.setStatus(customer.getStatus());
                    dto.setStatusText(getStatusText(customer.getStatus()));
                    dto.setTrialScheduleDate(history.getTrialScheduleDate());
                    dto.setTrialStartTime(history.getTrialStartTime());
                    dto.setTrialEndTime(history.getTrialEndTime());
                    dto.setTrialCoachId(history.getTrialCoachId());
                    dto.setHistoryId(history.getId());
                    dto.setTrialCancelled(history.getTrialCancelled());
                    dto.setCreatedAt(history.getCreatedAt());
                    
                    // 获取教练名称
                    if (history.getTrialCoachId() != null) {
                        Users coach = userRepository.findById(history.getTrialCoachId());
                        if (coach != null) {
                            dto.setTrialCoachName(coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
                        }
                    }
                    
                    // 获取创建人信息
                    if (history.getCreatedBy() != null) {
                        dto.setCreatedById(history.getCreatedBy());
                        Users creator = userRepository.findById(history.getCreatedBy());
                        if (creator != null) {
                            dto.setCreatedByName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
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

    @Transactional
    public CustomerDTO assignCustomer(Long customerId, Long assignedUserId, Long currentUserId, boolean isAdmin) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 检查权限：只有管理员或当前分配的销售/创建者可以分配客户
        if (!isAdmin && !currentUserId.equals(customer.getAssignedSalesId()) && !currentUserId.equals(customer.getCreatedBy())) {
            throw new RuntimeException("无权限分配此客户");
        }

        // 验证被分配的用户存在且属于同一机构
        Users assignedUser = userRepository.findById(assignedUserId);
        if (assignedUser == null) {
            throw new RuntimeException("被分配的用户不存在");
        }

        Users currentUser = userRepository.findById(currentUserId);
        if (currentUser != null && customer.getOrganizationId() != null && assignedUser.getOrganizationId() != null) {
            if (!customer.getOrganizationId().equals(assignedUser.getOrganizationId())) {
                throw new RuntimeException("只能分配给同一机构的用户");
            }
        }

        // 验证被分配的用户是销售或管理员
        if (!"SALES".equals(assignedUser.getPosition()) && !"ADMIN".equals(assignedUser.getRole())) {
            throw new RuntimeException("只能分配给销售职位或管理职位的用户");
        }

        // 更新分配销售ID
        customer.setAssignedSalesId(assignedUserId);
        Customer updatedCustomer = customerRepository.update(customer);
        
        return convertToDTO(updatedCustomer);
    }
}
