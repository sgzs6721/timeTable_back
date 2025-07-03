import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByTimetableId(Long timetableId);

    Optional<Schedule> findByTimetableIdAndDayOfWeekAndStartTimeAndEndTime(Long timetableId, String dayOfWeek, LocalTime startTime, LocalTime endTime);
    Optional<Schedule> findByTimetableIdAndScheduleDateAndStartTimeAndEndTime(Long timetableId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime);

    void deleteAllByTimetableIdAndDayOfWeekAndStartTimeAndEndTime(Long timetableId, String dayOfWeek, LocalTime startTime, LocalTime endTime);
    void deleteAllByTimetableIdAndScheduleDateAndStartTimeAndEndTime(Long timetableId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime);

    @Transactional
    void deleteByTimetableId(Long timetableId);
} 