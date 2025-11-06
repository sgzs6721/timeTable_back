package com.timetable.repository;

import com.timetable.entity.Todo;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.timetable.generated.Tables.TODOS;
import static com.timetable.generated.Tables.CUSTOMERS;

@Repository
public class TodoRepository extends BaseRepository {

    @Autowired
    private DSLContext dsl;

    public Todo create(Todo todo) {
        Long id = dsl.insertInto(TODOS)
                .set(TODOS.CUSTOMER_ID, todo.getCustomerId())
                .set(TODOS.CUSTOMER_NAME, todo.getCustomerName())
                .set(TODOS.CONTENT, todo.getContent())
                .set(TODOS.REMINDER_DATE, todo.getReminderDate())
                .set(TODOS.REMINDER_TIME, todo.getReminderTime())
                .set(TODOS.TYPE, todo.getType())
                .set(TODOS.STATUS, todo.getStatus())
                .set(TODOS.IS_READ, (byte) 0)
                .set(TODOS.CREATED_BY, todo.getCreatedBy())
                .set(TODOS.ORGANIZATION_ID, todo.getOrganizationId())
                .set(TODOS.CREATED_AT, LocalDateTime.now())
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .set(TODOS.DELETED, (byte) 0)
                .returning(TODOS.ID)
                .fetchOne()
                .getId();

        todo.setId(id);
        return findById(id);
    }

    public Todo findById(Long id) {
        return dsl.selectFrom(TODOS)
                .where(TODOS.ID.eq(id))
                .and(TODOS.DELETED.eq((byte) 0))
                .fetchOneInto(Todo.class);
    }

    public List<Todo> findByCreatedByAndOrganizationId(Long userId, Long organizationId) {
        return dsl.select(TODOS.fields())
                .from(TODOS)
                .leftJoin(CUSTOMERS).on(TODOS.CUSTOMER_ID.eq(CUSTOMERS.ID))
                .where(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.DELETED.eq((byte) 0))
                .and(
                    CUSTOMERS.ASSIGNED_SALES_ID.eq(userId)  // 客户分配给自己的待办
                    .or(TODOS.CUSTOMER_ID.isNull().and(TODOS.CREATED_BY.eq(userId)))  // 个人待办
                )
                .orderBy(TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
    }
    
    public List<Todo> findAllByOrganizationId(Long organizationId) {
        return dsl.selectFrom(TODOS)
                .where(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.DELETED.eq((byte) 0))
                .orderBy(TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
    }

    public List<Todo> findByCreatedByAndStatusAndOrganizationId(Long userId, String status, Long organizationId) {
        return dsl.select(TODOS.fields())
                .from(TODOS)
                .leftJoin(CUSTOMERS).on(TODOS.CUSTOMER_ID.eq(CUSTOMERS.ID))
                .where(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.STATUS.eq(status))
                .and(TODOS.DELETED.eq((byte) 0))
                .and(
                    CUSTOMERS.ASSIGNED_SALES_ID.eq(userId)  // 客户分配给自己的待办
                    .or(TODOS.CUSTOMER_ID.isNull().and(TODOS.CREATED_BY.eq(userId)))  // 个人待办
                )
                .orderBy(TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
    }
    
    public List<Todo> findAllByStatusAndOrganizationId(String status, Long organizationId) {
        return dsl.selectFrom(TODOS)
                .where(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.STATUS.eq(status))
                .and(TODOS.DELETED.eq((byte) 0))
                .orderBy(TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
    }

    public int countUnreadByCreatedByAndOrganizationId(Long userId, Long organizationId) {
        return dsl.selectCount()
                .from(TODOS)
                .leftJoin(CUSTOMERS).on(TODOS.CUSTOMER_ID.eq(CUSTOMERS.ID))
                .where(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.STATUS.ne("COMPLETED"))
                .and(TODOS.STATUS.ne("CANCELLED"))
                .and(TODOS.DELETED.eq((byte) 0))
                .and(
                    CUSTOMERS.ASSIGNED_SALES_ID.eq(userId)  // 客户分配给自己的待办
                    .or(TODOS.CUSTOMER_ID.isNull().and(TODOS.CREATED_BY.eq(userId)))  // 个人待办
                )
                .fetchOne(0, int.class);
    }
    
    public int countUnreadByOrganizationId(Long organizationId) {
        return dsl.selectCount()
                .from(TODOS)
                .where(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.STATUS.ne("COMPLETED"))
                .and(TODOS.STATUS.ne("CANCELLED"))
                .and(TODOS.DELETED.eq((byte) 0))
                .fetchOne(0, int.class);
    }

    public int markAsRead(Long id) {
        return dsl.update(TODOS)
                .set(TODOS.IS_READ, (byte) 1)
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .and(TODOS.DELETED.eq((byte) 0))
                .execute();
    }

    public int markAsCompleted(Long id) {
        return dsl.update(TODOS)
                .set(TODOS.STATUS, "COMPLETED")
                .set(TODOS.COMPLETED_AT, LocalDateTime.now())
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .and(TODOS.DELETED.eq((byte) 0))
                .execute();
    }

    public int markAsCancelled(Long id) {
        return dsl.update(TODOS)
                .set(TODOS.STATUS, "CANCELLED")
                .set(TODOS.CANCELLED_AT, LocalDateTime.now())
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .and(TODOS.DELETED.eq((byte) 0))
                .execute();
    }

    public int updateStatus(Long id, String status) {
        return dsl.update(TODOS)
                .set(TODOS.STATUS, status)
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .and(TODOS.DELETED.eq((byte) 0))
                .execute();
    }

    public int delete(Long id) {
        return dsl.update(TODOS)
                .set(TODOS.DELETED, (byte) 1)
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .execute();
    }

    public boolean existsByCustomerIdAndOrganizationId(Long customerId, Long organizationId) {
        Integer count = dsl.selectCount()
                .from(TODOS)
                .where(TODOS.CUSTOMER_ID.eq(customerId))
                .and(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.DELETED.eq((byte) 0))
                .and(TODOS.STATUS.ne("COMPLETED"))
                .and(TODOS.STATUS.ne("CANCELLED"))
                .fetchOne(0, Integer.class);
        return count != null && count > 0;
    }

    public Todo findLatestTodoByCustomerIdAndOrganizationId(Long customerId, Long organizationId) {
        return dsl.selectFrom(TODOS)
                .where(TODOS.CUSTOMER_ID.eq(customerId))
                .and(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.DELETED.eq((byte) 0))
                .and(TODOS.STATUS.ne("COMPLETED"))
                .and(TODOS.STATUS.ne("CANCELLED"))
                .orderBy(TODOS.CREATED_AT.desc())
                .limit(1)
                .fetchOneInto(Todo.class);
    }
    
    public List<Todo> findLatestTodosByCustomerIdsAndOrganizationId(List<Long> customerIds, Long organizationId) {
        if (customerIds == null || customerIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 查询所有待办
        List<Todo> allTodos = dsl.selectFrom(TODOS)
                .where(TODOS.CUSTOMER_ID.in(customerIds))
                .and(TODOS.ORGANIZATION_ID.eq(organizationId))
                .and(TODOS.DELETED.eq((byte) 0))
                .and(TODOS.STATUS.ne("COMPLETED"))
                .and(TODOS.STATUS.ne("CANCELLED"))
                .orderBy(TODOS.CUSTOMER_ID.asc(), TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
        
        // 按客户ID分组，每个客户只保留最新的一条
        java.util.Map<Long, Todo> latestByCustomer = new java.util.HashMap<>();
        for (Todo todo : allTodos) {
            Long customerId = todo.getCustomerId();
            if (customerId != null && !latestByCustomer.containsKey(customerId)) {
                latestByCustomer.put(customerId, todo);
            }
        }
        
        return new java.util.ArrayList<>(latestByCustomer.values());
    }

    public Todo update(Todo todo) {
        dsl.update(TODOS)
                .set(TODOS.CUSTOMER_ID, todo.getCustomerId())
                .set(TODOS.CUSTOMER_NAME, todo.getCustomerName())
                .set(TODOS.CONTENT, todo.getContent())
                .set(TODOS.REMINDER_DATE, todo.getReminderDate())
                .set(TODOS.REMINDER_TIME, todo.getReminderTime())
                .set(TODOS.TYPE, todo.getType())
                .set(TODOS.STATUS, todo.getStatus())
                .set(TODOS.ORGANIZATION_ID, todo.getOrganizationId())
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(todo.getId()))
                .and(TODOS.DELETED.eq((byte) 0))
                .execute();

        return findById(todo.getId());
    }

    /**
     * 查询需要推送的待办
     * 条件：提醒时间已到，状态为待推送或推送失败（重试次数未超限），未完成，未删除
     */
    public List<Todo> findTodosToPush(int maxRetryCount) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        return dsl.selectFrom(TODOS)
                .where(TODOS.DELETED.eq((byte) 0))
                .and(TODOS.STATUS.ne("COMPLETED"))
                .and(TODOS.REMINDER_DATE.isNotNull())
                .and(
                    // 提醒日期小于今天，或者等于今天且时间已到
                    TODOS.REMINDER_DATE.lt(currentDate)
                    .or(
                        TODOS.REMINDER_DATE.eq(currentDate)
                        .and(TODOS.REMINDER_TIME.isNotNull())
                        .and(TODOS.REMINDER_TIME.le(currentTime))
                    )
                )
                .and(
                    // 推送状态为 PENDING 或 FAILED（且重试次数未超限）
                    TODOS.PUSH_STATUS.eq("PENDING")
                    .or(
                        TODOS.PUSH_STATUS.eq("FAILED")
                        .and(TODOS.PUSH_RETRY_COUNT.lt(maxRetryCount))
                    )
                )
                .orderBy(TODOS.REMINDER_DATE.asc(), TODOS.REMINDER_TIME.asc())
                .fetchInto(Todo.class);
    }

    /**
     * 更新推送状态为成功
     */
    public int updatePushSuccess(Long id) {
        return dsl.update(TODOS)
                .set(TODOS.PUSH_STATUS, "PUSHED")
                .set(TODOS.PUSHED_AT, LocalDateTime.now())
                .set(TODOS.PUSH_ERROR_MESSAGE, (String) null)
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .execute();
    }

    /**
     * 更新推送状态为失败
     */
    public int updatePushFailed(Long id, String errorMessage) {
        return dsl.update(TODOS)
                .set(TODOS.PUSH_STATUS, "FAILED")
                .set(TODOS.PUSH_ERROR_MESSAGE, errorMessage)
                .set(TODOS.PUSH_RETRY_COUNT, TODOS.PUSH_RETRY_COUNT.plus(1))
                .set(TODOS.UPDATED_AT, LocalDateTime.now())
                .where(TODOS.ID.eq(id))
                .execute();
    }
}

