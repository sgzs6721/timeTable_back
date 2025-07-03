import com.example.timetable.dto.AiRequestDto;
import com.example.timetable.dto.ParseResultDto;
import com.example.timetable.dto.ScheduleCreateDto;
import com.example.timetable.dto.TextRequestDto;
import com.example.timetable.entity.Schedule;
import com.example.timetable.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/ai")
    public ResponseEntity<List<ScheduleCreateDto>> addScheduleByText(@RequestBody AiRequestDto aiRequestDto) {
        List<ScheduleCreateDto> schedules = scheduleService.parseTextWithAi(aiRequestDto.getText(), aiRequestDto.getType());
        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/{timetableId}/format")
    public ResponseEntity<ParseResultDto> addScheduleByFormat(
            @PathVariable Long timetableId,
            @RequestBody TextRequestDto textRequestDto) {
        ParseResultDto result = scheduleService.parseTextWithRules(textRequestDto.getText(), timetableId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{timetableId}/batch")
    public ResponseEntity<List<Schedule>> createSchedules(
            @PathVariable Long timetableId,
            @RequestBody List<ScheduleCreateDto> scheduleDtos) {
        List<Schedule> createdSchedules = scheduleService.createSchedules(timetableId, scheduleDtos);
        return new ResponseEntity<>(createdSchedules, HttpStatus.CREATED);
    }
} 