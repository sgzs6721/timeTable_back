package com.timetable.repository;

import org.jooq.DSLContext;
import com.timetable.generated.tables.daos.UsersDao;
import com.timetable.generated.tables.pojos.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * 根据用户名查找用户（只查找未删除且已批准的用户）
     */
    public Users findByUsername(String username) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq("APPROVED"))
                .fetchOneInto(Users.class);
    }
    
    /**
     * 根据昵称或用户名查找用户（只查找未删除且已批准的用户）
     */
    public Users findByNicknameOrUsername(String name) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.NICKNAME.eq(name)
                        .or(com.timetable.generated.tables.Users.USERS.USERNAME.eq(name)))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq("APPROVED"))
                .fetchOneInto(Users.class);
    }
    
    /**
     * 根据ID查找用户
     */
    public Users findById(Long id) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.ID.eq(id))
                .fetchOneInto(Users.class);
    }
    
    /**
     * 检查用户名是否存在（只检查未删除且已批准的用户）
     */
    public boolean existsByUsername(String username) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq("APPROVED"))
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

    /**
     * 根据机构ID查找用户
     */
    public List<Users> findByOrganizationId(Long organizationId) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ID.eq(organizationId))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchInto(Users.class);
    }
    
    /**
     * 获取所有活跃用户（未被软删除的用户）
     */
    public List<Users> findAllActiveUsers() {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 根据状态查找用户
     */
    public List<Users> findByStatus(String status) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.STATUS.eq(status))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 获取所有注册申请记录（包括已处理的）
     */
    public List<Users> findAllRegistrationRequests() {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.STATUS.in("PENDING", "APPROVED", "REJECTED"))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 根据机构ID获取所有注册申请记录（包括已处理的）
     */
    public List<Users> findAllRegistrationRequestsByOrganization(Long organizationId) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.STATUS.in("PENDING", "APPROVED", "REJECTED"))
                .and(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ID.eq(organizationId))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 根据用户名、删除状态和用户状态查找用户
     */
    public Users findByUsernameAndDeletedAndStatus(String username, Byte isDeleted, String status) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq(isDeleted))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq(status))
                .fetchOneInto(Users.class);
    }

    /**
     * 检查用户名在特定删除状态和用户状态下是否存在
     */
    public boolean existsByUsernameAndDeletedAndStatus(String username, Byte isDeleted, String status) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq(isDeleted))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq(status))
        );
    }

    /**
     * 查找所有具有相同用户名的用户（包括不同状态和删除状态）
     */
    public List<Users> findAllByUsername(String username) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.USERNAME.eq(username))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 获取所有APPROVED且未被软删除的用户
     */
    public List<Users> findAllApprovedUsers() {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq("APPROVED")
                        .or(com.timetable.generated.tables.Users.USERS.ROLE.eq("ADMIN")))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 根据微信OpenID查找用户（已废弃，使用 findByWechatOpenidAndOrganizationId）
     * @deprecated 由于支持多机构，一个openid可能有多条记录，请使用 findByWechatOpenidAndOrganizationId 或 findAllByWechatOpenid
     */
    @Deprecated
    public Users findByWechatOpenid(String wechatOpenid) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.WECHAT_OPENID.eq(wechatOpenid))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchOneInto(Users.class);
    }

    /**
     * 根据微信OpenID和机构ID查找用户
     */
    public Users findByWechatOpenidAndOrganizationId(String wechatOpenid, Long organizationId) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.WECHAT_OPENID.eq(wechatOpenid))
                .and(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ID.eq(organizationId))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchOneInto(Users.class);
    }

    /**
     * 根据微信OpenID查找该用户的所有机构记录
     */
    public List<Users> findAllByWechatOpenid(String wechatOpenid) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.WECHAT_OPENID.eq(wechatOpenid))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 根据微信OpenID查找该用户已批准的所有机构记录
     */
    public List<Users> findApprovedByWechatOpenid(String wechatOpenid) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.WECHAT_OPENID.eq(wechatOpenid))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq("APPROVED"))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .orderBy(com.timetable.generated.tables.Users.USERS.CREATED_AT.desc())
                .fetchInto(Users.class);
    }

    /**
     * 根据手机号查找用户
     */
    public Users findByPhone(String phone) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.PHONE.eq(phone))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchOneInto(Users.class);
    }

    /**
     * 更新用户手机号
     */
    public void updatePhone(Long userId, String phone) {
        dsl.update(com.timetable.generated.tables.Users.USERS)
                .set(com.timetable.generated.tables.Users.USERS.PHONE, phone)
                .set(com.timetable.generated.tables.Users.USERS.UPDATED_AT, java.time.LocalDateTime.now())
                .where(com.timetable.generated.tables.Users.USERS.ID.eq(userId))
                .execute();
    }

    /**
     * 检查手机号是否已被使用
     */
    public boolean existsByPhone(String phone) {
        return dsl.fetchExists(
            dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.PHONE.eq(phone))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
        );
    }

    /**
     * 统计特定角色的成员数量
     */
    public int countByOrganizationRoleId(Long roleId) {
        if (roleId == null) {
            return 0;
        }
        Integer count = dsl.selectCount()
                .from(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ROLE_ID.eq(roleId))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchOne(0, int.class);
        return count != null ? count : 0;
    }

    /**
     * 更新用户的机构角色
     */
    public void updateOrganizationRoleId(Long userId, Long roleId) {
        dsl.update(com.timetable.generated.tables.Users.USERS)
                .set(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ROLE_ID, roleId)
                .where(com.timetable.generated.tables.Users.USERS.ID.eq(userId))
                .execute();
    }

    /**
     * 根据机构角色ID查找用户
     */
    public List<Users> findByOrganizationRoleId(Long roleId) {
        return dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ROLE_ID.eq(roleId))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchInto(Users.class);
    }

    /**
     * 统计特定职位和机构的成员数量
     */
    public int countByPositionAndOrganizationId(String position, Long organizationId) {
        if (position == null || organizationId == null) {
            return 0;
        }
        Integer count = dsl.selectCount()
                .from(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.POSITION.eq(position))
                .and(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ID.eq(organizationId))
                .and(com.timetable.generated.tables.Users.USERS.STATUS.eq("APPROVED"))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchOne(0, int.class);
        return count != null ? count : 0;
    }
    
    /**
     * 检查用户是否是机构管理员
     */
    public boolean isOrganizationAdmin(Long userId, Long organizationId) {
        // 检查用户是否属于该机构并且是ADMIN角色
        Users user = dsl.selectFrom(com.timetable.generated.tables.Users.USERS)
                .where(com.timetable.generated.tables.Users.USERS.ID.eq(userId))
                .and(com.timetable.generated.tables.Users.USERS.ORGANIZATION_ID.eq(organizationId))
                .and(com.timetable.generated.tables.Users.USERS.ROLE.eq("ADMIN"))
                .and(com.timetable.generated.tables.Users.USERS.IS_DELETED.isNull()
                        .or(com.timetable.generated.tables.Users.USERS.IS_DELETED.eq((byte) 0)))
                .fetchOneInto(Users.class);
        
        return user != null;
    }
}