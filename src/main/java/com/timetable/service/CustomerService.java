package com.timetable.service;

import com.timetable.dto.CustomerDTO;
import com.timetable.dto.CustomerRequest;
import com.timetable.entity.Customer;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.repository.CustomerRepository;
import com.timetable.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CustomerDTO createCustomer(CustomerRequest request, Long currentUserId) {
        Customer customer = new Customer();
        customer.setChildName(request.getChildName());
        customer.setGrade(request.getGrade());
        customer.setParentPhone(request.getParentPhone());
        customer.setParentRelation(request.getParentRelation());
        customer.setAvailableTime(request.getAvailableTime());
        customer.setSource(request.getSource());
        customer.setStatus(request.getStatus() != null ? request.getStatus() : "NEW");
        customer.setNotes(request.getNotes());
        customer.setNextContactTime(request.getNextContactTime());
        customer.setVisitTime(request.getVisitTime());
        customer.setAssignedSalesId(request.getAssignedSalesId() != null ? request.getAssignedSalesId() : currentUserId);
        customer.setCreatedBy(currentUserId);

        Customer savedCustomer = customerRepository.save(customer);
        return convertToDTO(savedCustomer);
    }

    @Transactional
    public CustomerDTO updateCustomer(Long customerId, CustomerRequest request, Long currentUserId) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 检查权限：只有管理员或分配的销售可以修改
        if (!currentUserId.equals(customer.getAssignedSalesId()) && 
            !isAdmin(currentUserId)) {
            throw new RuntimeException("无权限修改此客户");
        }

        customer.setChildName(request.getChildName());
        customer.setGrade(request.getGrade());
        customer.setParentPhone(request.getParentPhone());
        customer.setParentRelation(request.getParentRelation());
        customer.setAvailableTime(request.getAvailableTime());
        customer.setSource(request.getSource());
        customer.setStatus(request.getStatus());
        customer.setNotes(request.getNotes());
        customer.setNextContactTime(request.getNextContactTime());
        customer.setVisitTime(request.getVisitTime());
        if (request.getAssignedSalesId() != null) {
            customer.setAssignedSalesId(request.getAssignedSalesId());
        }

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

    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setChildName(customer.getChildName());
        dto.setGrade(customer.getGrade());
        dto.setParentPhone(customer.getParentPhone());
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

        return dto;
    }

    private String getStatusText(String status) {
        switch (status) {
            case "NEW": return "新建";
            case "CONTACTED": return "已联系";
            case "SCHEDULED": return "已安排上门";
            case "PENDING_CONFIRM": return "待确认";
            case "VISITED": return "已上门";
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
}
