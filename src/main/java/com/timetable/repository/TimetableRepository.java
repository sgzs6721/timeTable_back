package com.timetable.repository;

import com.timetable.model.Timetable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 课表Repository - 内存实现（用于测试）
 */
@Repository
public class TimetableRepository {
    
    private final Map<Long, Timetable> timetables = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    /**
     * 根据用户ID查找课表列表
     */
    public List<Timetable> findByUserId(Long userId) {
        return timetables.values().stream()
                .filter(timetable -> userId.equals(timetable.getUserId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID查找课表
     */
    public Timetable findById(Long id) {
        return timetables.get(id);
    }
    
    /**
     * 根据ID和用户ID查找课表
     */
    public Timetable findByIdAndUserId(Long id, Long userId) {
        Timetable timetable = timetables.get(id);
        if (timetable != null && userId.equals(timetable.getUserId())) {
            return timetable;
        }
        return null;
    }
    
    /**
     * 保存课表
     */
    public Timetable save(Timetable timetable) {
        if (timetable.getId() == null) {
            timetable.setId(idGenerator.getAndIncrement());
            timetable.setCreatedAt(LocalDateTime.now());
        }
        timetable.setUpdatedAt(LocalDateTime.now());
        
        timetables.put(timetable.getId(), timetable);
        return timetable;
    }
    
    /**
     * 删除课表
     */
    public void deleteById(Long id) {
        timetables.remove(id);
    }
    
    /**
     * 检查课表是否存在
     */
    public boolean existsById(Long id) {
        return timetables.containsKey(id);
    }
    
    /**
     * 检查课表是否属于指定用户
     */
    public boolean existsByIdAndUserId(Long id, Long userId) {
        Timetable timetable = timetables.get(id);
        return timetable != null && userId.equals(timetable.getUserId());
    }
    
    /**
     * 获取所有课表（管理员功能）
     */
    public List<Timetable> findAll() {
        return timetables.values().stream().collect(Collectors.toList());
    }
    
    /**
     * 根据ID列表查找课表
     */
    public List<Timetable> findByIdIn(List<Long> ids) {
        return ids.stream()
                .map(timetables::get)
                .filter(timetable -> timetable != null)
                .collect(Collectors.toList());
    }
} 