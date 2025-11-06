package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.CustomerDTO;
import com.timetable.dto.CustomerRequest;
import com.timetable.dto.TrialCustomerDTO;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.CustomerService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDTO>> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            CustomerDTO customer = customerService.createCustomer(request, user.getId());
            return ResponseEntity.ok(ApiResponse.success("创建成功", customer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("创建客户失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> getCustomers(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) Long salesId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filterDate,
            @RequestParam(required = false) String keyword) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            if (user.getOrganizationId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户未关联机构"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            List<CustomerDTO> customers = customerService.getCustomersWithFilters(
                user.getId(), user.getOrganizationId(), isAdmin, page, pageSize, status, statuses, salesId, filterDate, keyword);
            return ResponseEntity.ok(ApiResponse.success("获取成功", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取客户列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomer(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            CustomerDTO customer = customerService.getCustomerById(id, user.getId(), isAdmin);
            return ResponseEntity.ok(ApiResponse.success("获取成功", customer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取客户详情失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            CustomerDTO customer = customerService.updateCustomer(id, request, user.getId());
            return ResponseEntity.ok(ApiResponse.success("更新成功", customer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("更新客户失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCustomer(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            customerService.deleteCustomer(id, user.getId(), isAdmin);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("删除客户失败: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> getCustomersByStatus(
            @PathVariable String status,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            if (user.getOrganizationId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户未关联机构"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            List<CustomerDTO> customers = customerService.getCustomersByStatus(status, user.getId(), user.getOrganizationId(), isAdmin);
            return ResponseEntity.ok(ApiResponse.success("获取成功", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取客户列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/trials")
    public ResponseEntity<ApiResponse<List<TrialCustomerDTO>>> getTrialCustomers(
            Authentication authentication,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate trialDate,
            @RequestParam(required = false, defaultValue = "false") boolean includeAll) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            if (user.getOrganizationId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户未分配机构"));
            }

            List<TrialCustomerDTO> trials = customerService.getTrialCustomers(
                user.getId(), 
                user.getOrganizationId(), 
                createdById, 
                trialDate,
                includeAll
            );
            return ResponseEntity.ok(ApiResponse.success("获取成功", trials));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取待体验客户列表失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{customerId}/assign")
    public ResponseEntity<ApiResponse<CustomerDTO>> assignCustomer(
            @PathVariable Long customerId,
            @RequestParam Long assignedUserId,
            Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            CustomerDTO customer = customerService.assignCustomer(customerId, assignedUserId, user.getId(), isAdmin);
            return ResponseEntity.ok(ApiResponse.success("分配成功", customer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("分配客户失败: " + e.getMessage()));
        }
    }
}
