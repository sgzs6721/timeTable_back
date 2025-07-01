package com.timetable.repository;

import org.jooq.DSLContext;
import com.timetable.generated.tables.daos.UsersDao;
import com.timetable.generated.tables.pojos.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 用户Repository - 数据库实现
 */
@Repository
public class UserRepository {
    
    @Autowired
    private DSLContext dsl;
    @Autowired
    private UsersDao usersDao;
    
    /**
     * 根据用户名查找用户
     */
    public Users findByUsername(String username) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
                .fetchOneInto(Users.class);
    }
    
    /**
     * 根据ID查找用户
     */
    public Users findById(Long id) {
        return usersDao.findById(id);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
        );
    }
    
    /**
     * 保存用户
     */
    public void save(Users user) {
        usersDao.insert(user);
    }
    
    /**
     * 更新用户
     */
    public void update(Users user) {
        usersDao.update(user);
    }
    
    /**
     * 删除用户
     */
    public void deleteById(Long id) {
        usersDao.deleteById(id);
    }
} 