package com.timetable.dto;
import java.util.List;

public class CoachStudentSummaryDTO {
    private Long coachId;
    private String coachName;
    private Integer totalCount;
    private List<StudentSummaryDTO> students;

    public CoachStudentSummaryDTO() {}

    public CoachStudentSummaryDTO(Long coachId, String coachName, Integer totalCount, List<StudentSummaryDTO> students) {
        this.coachId = coachId;
        this.coachName = coachName;
        this.totalCount = totalCount;
        this.students = students;
    }

    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    public String getCoachName() { return coachName; }
    public void setCoachName(String coachName) { this.coachName = coachName; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public List<StudentSummaryDTO> getStudents() { return students; }
    public void setStudents(List<StudentSummaryDTO> students) { this.students = students; }
}
