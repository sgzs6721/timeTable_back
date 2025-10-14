package com.timetable.dto;

/**
 * 学员汇总 DTO：用于返回学员名称与已上课程数
 */
public class StudentSummaryDTO {
    private Long studentId; // 学员ID
    private String studentName;
    private Integer attendedCount;

    public StudentSummaryDTO() {}

    public StudentSummaryDTO(String studentName, Integer attendedCount) {
        this.studentName = studentName;
        this.attendedCount = attendedCount;
    }
    
    public StudentSummaryDTO(Long studentId, String studentName, Integer attendedCount) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.attendedCount = attendedCount;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getAttendedCount() {
        return attendedCount;
    }

    public void setAttendedCount(Integer attendedCount) {
        this.attendedCount = attendedCount;
    }
}


