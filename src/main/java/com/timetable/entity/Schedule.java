package com.timetable.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules")
public class Schedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "课表ID不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    @JsonIgnore
    private Timetable timetable;
    
    @NotBlank(message = "学员姓名不能为空")
    @Size(min = 1, max = 100, message = "学员姓名长度必须在1-100个字符之间")
    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;
    
    @Size(max = 100, message = "科目名称长度不能超过100个字符")
    @Column(length = 100)
    private String subject;
    
    @NotNull(message = "星期不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;
    
    @NotNull(message = "开始时间不能为空")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @NotNull(message = "结束时间不能为空")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    
    @Column(name = "week_number")
    private Integer weekNumber;
    
    @Column(name = "schedule_date")
    private LocalDate scheduleDate;
    
    @Column(columnDefinition = "TEXT")
    private String note;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum DayOfWeek {
        MONDAY("周一"),
        TUESDAY("周二"),
        WEDNESDAY("周三"),
        THURSDAY("周四"),
        FRIDAY("周五"),
        SATURDAY("周六"),
        SUNDAY("周日");
        
        private final String displayName;
        
        DayOfWeek(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static DayOfWeek fromString(String dayStr) {
            if (dayStr == null) return null;
            
            switch (dayStr.toLowerCase()) {
                case "monday":
                case "周一":
                case "星期一":
                    return MONDAY;
                case "tuesday":
                case "周二":
                case "星期二":
                    return TUESDAY;
                case "wednesday":
                case "周三":
                case "星期三":
                    return WEDNESDAY;
                case "thursday":
                case "周四":
                case "星期四":
                    return THURSDAY;
                case "friday":
                case "周五":
                case "星期五":
                    return FRIDAY;
                case "saturday":
                case "周六":
                case "星期六":
                    return SATURDAY;
                case "sunday":
                case "周日":
                case "星期日":
                    return SUNDAY;
                default:
                    return null;
            }
        }
    }
    
    // 构造函数
    public Schedule() {
    }
    
    public Schedule(Timetable timetable, String studentName, String subject, 
                   DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.timetable = timetable;
        this.studentName = studentName;
        this.subject = subject;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Timetable getTimetable() {
        return timetable;
    }
    
    public void setTimetable(Timetable timetable) {
        this.timetable = timetable;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getWeekNumber() {
        return weekNumber;
    }
    
    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }
    
    public LocalDate getScheduleDate() {
        return scheduleDate;
    }
    
    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 