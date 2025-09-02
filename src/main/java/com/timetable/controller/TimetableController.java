package com.timetable.controller;

import com.timetable.dto.AdminTimetableDTO;
import com.timetable.dto.ApiResponse;
import com.timetable.dto.BatchTimetableRequest;
import com.timetable.dto.TimetableRequest;
import com.timetable.generated.tables.pojos.Timetables;
import com.timetable.generated.tables.pojos.Users;
import com.timetable.service.TimetableService;
import com.timetable.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课表控制器
 */
@RestController
@RequestMapping("/timetables")
@Validated
public class TimetableController {

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private UserService userService;

    /**
     * 获取用户的课表列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Timetables>>> getUserTimetables(Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        List<Timetables> timetables = timetableService.getUserTimetables(user.getId());
        return ResponseEntity.ok(ApiResponse.success("获取课表列表成功", timetables));
    }

    /**
     * 获取日期范围课表内（按周一至周日）各周的课程数量列表，仅用于转换时选择有课的周
     */
    @GetMapping("/{id}/weeks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWeeksWithCounts(
            @PathVariable Long id,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        Timetables timetable;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            timetable = timetableService.getTimetableById(id);
        } else {
            timetable = timetableService.getTimetable(id, user.getId());
        }

        if (timetable == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> list = timetableService.getWeeksWithCounts(id);
        return ResponseEntity.ok(ApiResponse.success("获取周列表成功", list));
    }

    /**
     * 创建新课表
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Timetables>> createTimetable(
            @Valid @RequestBody TimetableRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        // 校验课表类型和日期
        if (request.getType() == TimetableRequest.TimetableType.DATE_RANGE) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("日期范围课表必须指定开始和结束日期"));
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始日期不能晚于结束日期"));
            }
        }
        try {
            Timetables timetable = timetableService.createTimetable(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("创建课表成功", timetable));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取课表详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTimetable(
            @PathVariable Long id,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        Timetables timetable;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            timetable = timetableService.getTimetableById(id);
        } else {
            timetable = timetableService.getTimetable(id, user.getId());
        }

        if (timetable == null) {
            return ResponseEntity.notFound()
                    .build();
        }

        // 获取课表所属用户信息
        Users timetableOwner = userService.findById(timetable.getUserId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timetable", timetable);
        if (timetableOwner != null) {
            Map<String, Object> ownerInfo = new HashMap<>();
            ownerInfo.put("id", timetableOwner.getId());
            ownerInfo.put("username", timetableOwner.getUsername());
            ownerInfo.put("nickname", timetableOwner.getNickname());
            response.put("owner", ownerInfo);
        }

        return ResponseEntity.ok(ApiResponse.success("获取课表详情成功", response));
    }

    /**
     * 更新课表
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Timetables>> updateTimetable(
            @PathVariable Long id,
            @Valid @RequestBody TimetableRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }
        // 校验课表类型和日期
        if (request.getType() == TimetableRequest.TimetableType.DATE_RANGE) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("日期范围课表必须指定开始和结束日期"));
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("开始日期不能晚于结束日期"));
            }
        }
        Timetables timetable = timetableService.updateTimetable(id, user.getId(), request);
        if (timetable == null) {
            return ResponseEntity.notFound()
                    .build();
        }
        return ResponseEntity.ok(ApiResponse.success("更新课表成功", timetable));
    }

    /**
     * 删除课表
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTimetable(
            @PathVariable Long id,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户不存在"));
        }

        boolean deleted = timetableService.deleteTimetable(id, user.getId());

        if (!deleted) {
            return ResponseEntity.notFound()
                    .build();
        }

        return ResponseEntity.ok(ApiResponse.success("课表删除成功"));
    }

    /**
     * 设为活动课表
     */
    @PutMapping("/{id}/active")
    public ResponseEntity<ApiResponse<String>> setActiveTimetable(
            @PathVariable Long id,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        boolean ok = timetableService.setActiveTimetable(id, user.getId());
        if (!ok) {
            return ResponseEntity.badRequest().body(ApiResponse.error("设置失败，课表不存在或已删除"));
        }
        return ResponseEntity.ok(ApiResponse.success("已设为活动课表"));
    }

    /**
     * 归档课表
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<String>> archiveTimetable(
            @PathVariable Long id,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        boolean ok = timetableService.archiveTimetable(id, user.getId());
        if (!ok) {
            return ResponseEntity.badRequest().body(ApiResponse.error("归档失败，课表不存在或已删除"));
        }
        return ResponseEntity.ok(ApiResponse.success("课表已归档"));
    }

    /**
     * 恢复归档课表
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<String>> restoreTimetable(
            @PathVariable Long id,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        try {
            boolean ok = timetableService.restoreTimetable(id, user.getId());
            if (!ok) {
                return ResponseEntity.badRequest().body(ApiResponse.error("恢复失败，课表不存在或已删除"));
            }
            return ResponseEntity.ok(ApiResponse.success("课表已恢复"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取归档课表列表
     */
    @GetMapping("/archived")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getArchivedTimetables(Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        List<AdminTimetableDTO> list;

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            list = timetableService.getAllTimetablesWithUser().stream()
                    .filter(t -> t.getIsArchived() != null && t.getIsArchived() == 1)
                    .collect(Collectors.toList());
        } else {
            list = timetableService.findArchivedByUserId(user.getId());
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("archivedList", list);

        return ResponseEntity.ok(ApiResponse.success("获取归档课表成功", responseData));
    }

    /**
     * 批量恢复归档课表
     */
    @PostMapping("/batch-restore")
    public ResponseEntity<ApiResponse<String>> batchRestoreTimetables(
            @Valid @RequestBody BatchTimetableRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        try {
            int count = timetableService.batchRestoreTimetables(request.getIds(), user.getId());
            return ResponseEntity.ok(ApiResponse.success(count + " 个课表已恢复"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量彻底删除课表
     */
    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<String>> batchDeleteTimetables(
            @Valid @RequestBody BatchTimetableRequest request,
            Authentication authentication) {
        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        int count = timetableService.batchDeleteTimetables(request.getIds(), user.getId());
        return ResponseEntity.ok(ApiResponse.success(count + " 个课表已删除"));
    }

    /**
     * 将日期范围课表按选中周转为周固定课表
     */
    @PostMapping("/{id}/convert/date-to-weekly")
    public ResponseEntity<ApiResponse<String>> convertDateToWeekly(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        String weekStartStr = body.get("weekStart");
        if (weekStartStr == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("缺少weekStart参数"));
        }

        try {
            java.time.LocalDate weekStart = java.time.LocalDate.parse(weekStartStr);
            timetableService.convertDateRangeToWeekly(id, weekStart);
            return ResponseEntity.ok(ApiResponse.success("转换为周固定课表成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("转换失败: " + e.getMessage()));
        }
    }

    /**
     * 将周固定课表应用到日期范围，转换为日期类课表
     */
    @PostMapping("/{id}/convert/weekly-to-date")
    public ResponseEntity<ApiResponse<String>> convertWeeklyToDate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        Users user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }

        String startDateStr = body.get("startDate");
        String endDateStr = body.get("endDate");
        if (startDateStr == null || endDateStr == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("缺少起止日期"));
        }

        try {
            java.time.LocalDate startDate = java.time.LocalDate.parse(startDateStr);
            java.time.LocalDate endDate = java.time.LocalDate.parse(endDateStr);
            timetableService.convertWeeklyToDateRange(id, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("转换为日期范围课表成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("转换失败: " + e.getMessage()));
        }
    }
}