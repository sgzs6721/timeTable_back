package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.CustomerDTO;
import com.timetable.dto.CustomerRequest;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.CustomerService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> getCustomers(Authentication authentication) {
        try {
            Users user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }

            boolean isAdmin = "ADMIN".equals(user.getRole());
            List<CustomerDTO> customers = customerService.getCustomersForUser(user.getId(), isAdmin);
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

            boolean isAdmin = "ADMIN".equals(user.getRole());
            List<CustomerDTO> customers = customerService.getCustomersByStatus(status, user.getId(), isAdmin);
            return ResponseEntity.ok(ApiResponse.success("获取成功", customers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取客户列表失败: " + e.getMessage()));
        }
    }
}
