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
    
    @Autowired
    private com.timetable.repository.ScheduleRepository scheduleRepository;
    
    @Autowired
    private com.timetable.repository.WeeklyInstanceScheduleRepository weeklyInstanceScheduleRepository;

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
        boolean isAdmin = user != null && "MANAGER".equals(user.getPosition());
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
        history.setOrganizationId(customer.getOrganizationId());
        
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
        history.setTrialStudentName(request.getTrialStudentName());

        // 先保存流转记录
        CustomerStatusHistory savedHistory = historyRepository.save(history);
        
        // 如果有教练ID，说明需要创建课表中的课程
        if (request.getTrialCoachId() != null && 
            request.getTrialScheduleDate() != null && !request.getTrialScheduleDate().isEmpty() &&
            request.getTrialStartTime() != null && !request.getTrialStartTime().isEmpty() &&
            request.getTrialEndTime() != null && !request.getTrialEndTime().isEmpty() &&
            request.getTrialStudentName() != null && !request.getTrialStudentName().isEmpty()) {
            
            try {
                System.out.println("=== 创建体验课程到课表 ===");
                System.out.println("客户ID: " + customerId);
                System.out.println("教练ID: " + request.getTrialCoachId());
                System.out.println("学员姓名: " + request.getTrialStudentName());
                System.out.println("体验日期: " + request.getTrialScheduleDate());
                System.out.println("体验时间: " + request.getTrialStartTime() + " - " + request.getTrialEndTime());
                
                // 构建体验课程请求
                com.timetable.dto.TrialScheduleRequest trialRequest = new com.timetable.dto.TrialScheduleRequest();
                trialRequest.setCoachId(request.getTrialCoachId());
                trialRequest.setScheduleDate(request.getTrialScheduleDate());
                trialRequest.setStartTime(request.getTrialStartTime());
                trialRequest.setEndTime(request.getTrialEndTime());
                trialRequest.setStudentName(request.getTrialStudentName());
                trialRequest.setCustomerPhone(customer.getParentPhone());
                trialRequest.setIsTrial(true);
                trialRequest.setCustomerId(customerId);
                
                // 创建课表中的课程
                com.timetable.dto.TrialScheduleInfo scheduleInfo = scheduleService.createTrialSchedule(trialRequest, user);
                
                System.out.println("体验课程创建成功");
                System.out.println("课程ID: " + scheduleInfo.getScheduleId());
                System.out.println("课表ID: " + scheduleInfo.getTimetableId());
                System.out.println("来源类型: " + scheduleInfo.getSourceType());
                
                // 更新流转记录，保存课程ID等信息
                savedHistory.setTrialScheduleId(scheduleInfo.getScheduleId());
                savedHistory.setTrialTimetableId(scheduleInfo.getTimetableId());
                savedHistory.setTrialSourceType(scheduleInfo.getSourceType());
                historyRepository.updateTrialScheduleInfo(
                    savedHistory.getId(), 
                    scheduleInfo.getScheduleId(), 
                    scheduleInfo.getTimetableId(), 
                    scheduleInfo.getSourceType()
                );
                
                System.out.println("历史记录已更新，保存了课程关联信息");
                
            } catch (Exception e) {
                // 创建课表课程失败，抛出异常，事务回滚
                System.err.println("创建体验课程到课表失败: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("创建体验课程到课表失败: " + e.getMessage(), e);
            }
        } else {
            System.out.println("未提供完整的体验课程信息，不创建课表课程");
        }
        
        // 如果状态变更为VISITED（已体验），自动将该客户之前的待体验历史记录标记为已完成
        if ("VISITED".equals(toStatus) && ("SCHEDULED".equals(fromStatus) || "RE_EXPERIENCE".equals(fromStatus))) {
            try {
                // 查找该客户最近的待体验历史记录
                List<CustomerStatusHistory> histories = historyRepository.findByCustomerId(customerId);
                for (CustomerStatusHistory h : histories) {
                    // 找到待体验或待再体验状态的历史记录，且有体验时间信息，且未完成且未取消
                    if (("SCHEDULED".equals(h.getToStatus()) || "RE_EXPERIENCE".equals(h.getToStatus())) &&
                        h.getTrialScheduleDate() != null &&
                        (h.getTrialCompleted() == null || !h.getTrialCompleted()) &&
                        (h.getTrialCancelled() == null || !h.getTrialCancelled())) {
                        // 标记为已完成
                        historyRepository.markTrialAsCompleted(h.getId());
                        System.out.println("自动标记体验课程为已完成: historyId=" + h.getId());
                    }
                }
            } catch (Exception e) {
                System.err.println("自动标记体验课程完成失败: " + e.getMessage());
                // 不影响主流程，只记录错误
            }
        }
        
        return convertToDTO(savedHistory);
    }

    public List<CustomerStatusHistoryDTO> getHistoryByCustomerId(Long customerId, Long currentUserId) {
        Customer customer = customerRepository.findById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 检查权限
        Users user = userRepository.findById(currentUserId);
        boolean isAdmin = user != null && "MANAGER".equals(user.getPosition());
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
        boolean isAdmin = user != null && "MANAGER".equals(user.getPosition());
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
        boolean isAdmin = user != null && "MANAGER".equals(user.getPosition());
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
        dto.setTrialCompleted(history.getTrialCompleted());
        
        // 获取教练姓名
        if (history.getTrialCoachId() != null) {
            Users coach = userRepository.findById(history.getTrialCoachId());
            if (coach != null) {
                dto.setTrialCoachName(coach.getNickname() != null ? coach.getNickname() : coach.getUsername());
            }
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
     * 标记体验课程为已完成
     */
    public boolean markTrialAsCompleted(Long historyId) {
        try {
            return historyRepository.markTrialAsCompleted(historyId);
        } catch (Exception e) {
            throw new RuntimeException("标记体验课程完成失败: " + e.getMessage(), e);
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
        
        System.out.println("=== 取消体验课程 ===");
        System.out.println("历史记录ID: " + historyId);
        System.out.println("客户ID: " + history.getCustomerId());
        System.out.println("教练ID: " + history.getTrialCoachId());
        System.out.println("体验课程ID: " + history.getTrialScheduleId());
        System.out.println("课表ID: " + history.getTrialTimetableId());
        System.out.println("课程来源类型: " + history.getTrialSourceType());
        System.out.println("学员名称: " + history.getTrialStudentName());
        
        // 2. 标记历史记录为已取消
        boolean marked = historyRepository.markTrialAsCancelled(historyId);
        if (!marked) {
            throw new RuntimeException("标记体验课程取消失败");
        }
        
        // 3. 只有当体验课程关联了教练时，才从课表中删除
        // 如果没有关联教练，说明这个体验课程没有安排到课表里，只需要标记为已取消即可
        Long trialCoachId = history.getTrialCoachId();
        
        if (trialCoachId != null) {
            // 有关联教练，需要从课表中删除课程
            Long trialScheduleId = history.getTrialScheduleId();
            Long trialTimetableId = history.getTrialTimetableId();
            String sourceType = history.getTrialSourceType();
            
            if (trialScheduleId != null && sourceType != null) {
                try {
                    System.out.println("准备删除课表中的课程，来源类型: " + sourceType);
                    if ("weekly_instance".equals(sourceType)) {
                        // 删除周实例课程
                        System.out.println("删除周实例课程，ID: " + trialScheduleId);
                        weeklyInstanceService.deleteInstanceSchedule(trialScheduleId);
                        System.out.println("周实例课程删除成功");
                    } else if ("schedule".equals(sourceType)) {
                        // 删除普通课程，需要 timetableId
                        if (trialTimetableId == null) {
                            throw new RuntimeException("删除普通课程时缺少 timetableId");
                        }
                        System.out.println("删除普通课程，课表ID: " + trialTimetableId + ", 课程ID: " + trialScheduleId);
                        boolean deleted = scheduleService.deleteSchedule(trialTimetableId, trialScheduleId);
                        if (!deleted) {
                            throw new RuntimeException("删除课表中的课程失败");
                        }
                        System.out.println("普通课程删除成功");
                    }
                } catch (Exception e) {
                    // 删除课表失败，抛出异常触发事务回滚
                    System.err.println("删除课表中的体验课程失败: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("删除课表中的体验课程失败: " + e.getMessage(), e);
                }
            } else {
                // trial_schedule_id 或 source_type 为 null，说明课程信息未正确保存
                // 尝试使用兜底方案：通过学员姓名、时间等信息查找并删除课程
                System.err.println("警告：体验课程关联了教练但课程ID或来源类型为空");
                System.err.println("trialScheduleId: " + trialScheduleId + ", sourceType: " + sourceType);
                System.out.println("尝试使用兜底方案：通过学员姓名、时间等信息查找并删除课程");
                
                String studentName = history.getTrialStudentName();
                java.time.LocalDate scheduleDate = history.getTrialScheduleDate();
                java.time.LocalTime startTime = history.getTrialStartTime();
                java.time.LocalTime endTime = history.getTrialEndTime();
                
                if (studentName != null && !studentName.isEmpty() && 
                    scheduleDate != null && startTime != null && endTime != null) {
                    
                    boolean foundAndDeleted = false;
                    
                    try {
                        // 1. 在普通课表中查找体验课程
                        System.out.println("在普通课表中查找学员: " + studentName);
                        List<java.util.Map<String, Object>> trialSchedules = scheduleRepository.findTrialSchedulesByStudentName(studentName);
                        
                        for (java.util.Map<String, Object> schedule : trialSchedules) {
                            java.time.LocalDate schedDate = schedule.get("schedule_date") != null ? 
                                ((java.sql.Date) schedule.get("schedule_date")).toLocalDate() : null;
                            java.time.LocalTime sTime = schedule.get("start_time") != null ?
                                ((java.sql.Time) schedule.get("start_time")).toLocalTime() : null;
                            java.time.LocalTime eTime = schedule.get("end_time") != null ?
                                ((java.sql.Time) schedule.get("end_time")).toLocalTime() : null;
                            Long coachId = schedule.get("coach_id") != null ?
                                ((Number) schedule.get("coach_id")).longValue() : null;
                            
                            // 匹配日期、时间和教练
                            if (schedDate != null && schedDate.equals(scheduleDate) &&
                                sTime != null && sTime.equals(startTime) &&
                                eTime != null && eTime.equals(endTime) &&
                                coachId != null && coachId.equals(trialCoachId)) {
                                
                                Long scheduleId = ((Number) schedule.get("id")).longValue();
                                Long timetableId = ((Number) schedule.get("timetable_id")).longValue();
                                
                                System.out.println("找到匹配的普通课程，ID: " + scheduleId + ", 课表ID: " + timetableId);
                                boolean deleted = scheduleService.deleteSchedule(timetableId, scheduleId);
                                if (deleted) {
                                    System.out.println("通过兜底方案成功删除普通课程");
                                    foundAndDeleted = true;
                                    break;
                                }
                            }
                        }
                        
                        // 2. 如果在普通课表中没找到，在周实例中查找
                        if (!foundAndDeleted) {
                            System.out.println("在周实例中查找学员: " + studentName);
                            List<java.util.Map<String, Object>> instanceSchedules = scheduleRepository.findTrialSchedulesInInstancesByStudentName(studentName);
                            
                            for (java.util.Map<String, Object> schedule : instanceSchedules) {
                                java.time.LocalDate schedDate = schedule.get("schedule_date") != null ? 
                                    ((java.sql.Date) schedule.get("schedule_date")).toLocalDate() : null;
                                java.time.LocalTime sTime = schedule.get("start_time") != null ?
                                    ((java.sql.Time) schedule.get("start_time")).toLocalTime() : null;
                                java.time.LocalTime eTime = schedule.get("end_time") != null ?
                                    ((java.sql.Time) schedule.get("end_time")).toLocalTime() : null;
                                Long coachId = schedule.get("coach_id") != null ?
                                    ((Number) schedule.get("coach_id")).longValue() : null;
                                
                                // 匹配日期、时间和教练
                                if (schedDate != null && schedDate.equals(scheduleDate) &&
                                    sTime != null && sTime.equals(startTime) &&
                                    eTime != null && eTime.equals(endTime) &&
                                    coachId != null && coachId.equals(trialCoachId)) {
                                    
                                    Long scheduleId = ((Number) schedule.get("id")).longValue();
                                    
                                    System.out.println("找到匹配的周实例课程，ID: " + scheduleId);
                                    weeklyInstanceService.deleteInstanceSchedule(scheduleId);
                                    System.out.println("通过兜底方案成功删除周实例课程");
                                    foundAndDeleted = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!foundAndDeleted) {
                            System.err.println("兜底方案：未找到匹配的体验课程");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("兜底方案执行失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("缺少必要的信息（学员姓名、日期或时间），无法使用兜底方案");
                }
            }
        } else {
            System.out.println("体验课程未关联教练，无需删除课表");
        }
        
        return true;
    }

    /**
     * 更新体验时间
     */
    @Transactional
    public CustomerStatusHistoryDTO updateTrialTime(Long customerId, Long historyId, 
                                                     String trialScheduleDate, 
                                                     String trialStartTime, 
                                                     String trialEndTime,
                                                     String trialCoachName,
                                                     Long currentUserId) {
        // 获取历史记录
        CustomerStatusHistory history = historyRepository.findById(historyId);
        if (history == null) {
            throw new RuntimeException("历史记录不存在");
        }

        // 检查权限
        Users user = userRepository.findById(currentUserId);
        boolean isAdmin = user != null && "MANAGER".equals(user.getPosition());
        if (!isAdmin && !currentUserId.equals(history.getCreatedBy())) {
            throw new RuntimeException("无权限修改此历史记录");
        }

        // 检查客户ID是否匹配
        if (!customerId.equals(history.getCustomerId())) {
            throw new RuntimeException("客户ID不匹配");
        }

        // 检查体验课程是否已取消或已完成
        if (history.getTrialCancelled() != null && history.getTrialCancelled()) {
            throw new RuntimeException("体验课程已取消，无法修改时间");
        }
        if (history.getTrialCompleted() != null && history.getTrialCompleted()) {
            throw new RuntimeException("体验课程已完成，无法修改时间");
        }

        try {
            // 解析新的时间
            java.time.LocalDate newScheduleDate = java.time.LocalDate.parse(trialScheduleDate);
            java.time.LocalTime newStartTime = java.time.LocalTime.parse(trialStartTime);
            java.time.LocalTime newEndTime = java.time.LocalTime.parse(trialEndTime);

            // 如果提供了教练名称，查找教练ID并更新
            Long newCoachId = history.getTrialCoachId();
            String newCoachName = history.getTrialCoachName();
            if (trialCoachName != null && !trialCoachName.isEmpty()) {
                // 根据教练名称查找教练
                Users coach = userRepository.findByNicknameOrUsername(trialCoachName);
                if (coach != null) {
                    newCoachId = coach.getId();
                    newCoachName = coach.getNickname() != null ? coach.getNickname() : coach.getUsername();
                }
            }

            // 如果之前有关联的课表课程，需要删除旧的课程
            if (history.getTrialScheduleId() != null) {
                try {
                    // 根据来源类型删除课程
                    if ("weekly_instance".equals(history.getTrialSourceType())) {
                        // 删除周实例课程
                        weeklyInstanceScheduleRepository.delete(history.getTrialScheduleId());
                    } else if ("schedule".equals(history.getTrialSourceType())) {
                        // 删除普通课表课程
                        scheduleRepository.deleteById(history.getTrialScheduleId());
                    }
                } catch (Exception e) {
                    System.err.println("删除旧课程失败: " + e.getMessage());
                    // 继续执行，不影响主流程
                }
            }

            // 创建新的体验课程
            if (newCoachId != null && history.getTrialStudentName() != null) {
                try {
                    // 构建体验课程请求
                    com.timetable.dto.TrialScheduleRequest trialRequest = new com.timetable.dto.TrialScheduleRequest();
                    trialRequest.setCoachId(newCoachId);
                    trialRequest.setScheduleDate(trialScheduleDate);
                    trialRequest.setStartTime(trialStartTime);
                    trialRequest.setEndTime(trialEndTime);
                    trialRequest.setStudentName(history.getTrialStudentName());
                    trialRequest.setIsTrial(true);
                    trialRequest.setCustomerId(customerId);

                    // 获取客户电话
                    Customer customer = customerRepository.findById(customerId);
                    if (customer != null && customer.getParentPhone() != null) {
                        trialRequest.setCustomerPhone(customer.getParentPhone());
                    }

                    // 创建新的课表课程
                    com.timetable.dto.TrialScheduleInfo scheduleInfo = scheduleService.createTrialSchedule(trialRequest, user);

                    // 更新历史记录中的时间、教练和课程信息
                    history.setTrialScheduleDate(newScheduleDate);
                    history.setTrialStartTime(newStartTime);
                    history.setTrialEndTime(newEndTime);
                    history.setTrialCoachId(newCoachId);
                    history.setTrialCoachName(newCoachName);
                    history.setTrialScheduleId(scheduleInfo.getScheduleId());
                    history.setTrialTimetableId(scheduleInfo.getTimetableId());
                    history.setTrialSourceType(scheduleInfo.getSourceType());

                } catch (Exception e) {
                    System.err.println("创建新体验课程失败: " + e.getMessage());
                    throw new RuntimeException("创建新体验课程失败: " + e.getMessage());
                }
            } else {
                // 只更新时间和教练，不创建课程
                history.setTrialScheduleDate(newScheduleDate);
                history.setTrialStartTime(newStartTime);
                history.setTrialEndTime(newEndTime);
                if (newCoachId != null) {
                    history.setTrialCoachId(newCoachId);
                    history.setTrialCoachName(newCoachName);
                }
            }

            // 保存更新
            CustomerStatusHistory updatedHistory = historyRepository.update(history);
            return convertToDTO(updatedHistory);

        } catch (java.time.format.DateTimeParseException e) {
            throw new RuntimeException("时间格式错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("更新体验时间失败: " + e.getMessage());
            throw new RuntimeException("更新体验时间失败: " + e.getMessage());
        }
    }
}

