import com.example.timetable.dto.ConflictDto;
import com.example.timetable.dto.ParseResultDto;
import com.example.timetable.dto.ScheduleCreateDto;
import com.example.timetable.entity.Schedule;
import com.example.timetable.entity.Timetable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private AiNlpService aiNlpService;

    @Autowired
    private Validator validator;

    public ParseResultDto parseTextWithRules(String text, Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found with id: " + timetableId));

        List<ScheduleCreateDto> parsedSchedules = new ArrayList<>();
        String[] lines = text.split("\\n");
        // ... existing code ...
        // ... a lot of parsing logic ...
        // ...
        // After parsing is done and `parsedSchedules` is populated:

        List<ScheduleCreateDto> schedulesToConfirm = new ArrayList<>();
        List<ConflictDto> conflicts = new ArrayList<>();

        for (ScheduleCreateDto newSchedule : parsedSchedules) {
            Optional<Schedule> existingScheduleOpt;
            if (timetable.getIsWeekly()) {
                existingScheduleOpt = scheduleRepository.findByTimetableIdAndDayOfWeekAndStartTimeAndEndTime(
                        timetableId, newSchedule.getDayOfWeek(), newSchedule.getStartTime(), newSchedule.getEndTime());
            } else {
                existingScheduleOpt = scheduleRepository.findByTimetableIdAndScheduleDateAndStartTimeAndEndTime(
                        timetableId, newSchedule.getScheduleDate(), newSchedule.getStartTime(), newSchedule.getEndTime());
            }

            if (existingScheduleOpt.isPresent()) {
                conflicts.add(new ConflictDto(newSchedule, existingScheduleOpt.get()));
            } else {
                schedulesToConfirm.add(newSchedule);
            }
        }

        return new ParseResultDto(schedulesToConfirm, conflicts);
    }
    
    @Transactional
    public List<Schedule> createSchedules(Long timetableId, List<ScheduleCreateDto> scheduleDtos) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found with id: " + timetableId));

        // Validate all DTOs first
        for (ScheduleCreateDto dto : scheduleDtos) {
            if (timetable.getIsWeekly()) {
                Set<ConstraintViolation<ScheduleCreateDto>> violations = validator.validate(dto, ValidationGroup.Weekly.class);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
            } else {
                Set<ConstraintViolation<ScheduleCreateDto>> violations = validator.validate(dto, ValidationGroup.Daily.class);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
            }
        }

        List<Schedule> savedSchedules = new ArrayList<>();

        for (ScheduleCreateDto dto : scheduleDtos) {
            // Delete existing schedules in the same slot before saving the new one
            if (timetable.getIsWeekly()) {
                scheduleRepository.deleteAllByTimetableIdAndDayOfWeekAndStartTimeAndEndTime(
                    timetableId, dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime());
            } else {
                scheduleRepository.deleteAllByTimetableIdAndScheduleDateAndStartTimeAndEndTime(
                    timetableId, dto.getScheduleDate(), dto.getStartTime(), dto.getEndTime());
            }

            Schedule schedule = new Schedule();
            schedule.setTimetable(timetable);
            schedule.setStudentName(dto.getStudentName());
            schedule.setDayOfWeek(dto.getDayOfWeek());
            schedule.setScheduleDate(dto.getScheduleDate());
            schedule.setStartTime(dto.getStartTime());
            schedule.setEndTime(dto.getEndTime());
            savedSchedules.add(scheduleRepository.save(schedule));
        }

        return savedSchedules;
    }

} 