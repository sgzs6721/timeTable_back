package com.timetable.dto;

/**
 * 学员汇总 DTO：用于返回学员名称与已上课程数
 */
public class StudentSummaryDTO {
    private String studentName;
    private Integer attendedCount;

    public StudentSummaryDTO() {}

    public StudentSummaryDTO(String studentName, Integer attendedCount) {
        this.studentName = studentName;
        this.attendedCount = attendedCount;
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


