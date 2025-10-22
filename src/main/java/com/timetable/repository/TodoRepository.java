package com.timetable.repository;

import com.timetable.entity.Todo;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.timetable.generated.Tables.TODOS;

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

    public List<Todo> findByCreatedBy(Long userId) {
        return dsl.selectFrom(TODOS)
                .where(TODOS.CREATED_BY.eq(userId))
                .and(TODOS.DELETED.eq((byte) 0))
                .orderBy(TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
    }

    public List<Todo> findByCreatedByAndStatus(Long userId, String status) {
        return dsl.selectFrom(TODOS)
                .where(TODOS.CREATED_BY.eq(userId))
                .and(TODOS.STATUS.eq(status))
                .and(TODOS.DELETED.eq((byte) 0))
                .orderBy(TODOS.CREATED_AT.desc())
                .fetchInto(Todo.class);
    }

    public int countUnreadByCreatedBy(Long userId) {
        return dsl.selectCount()
                .from(TODOS)
                .where(TODOS.CREATED_BY.eq(userId))
                .and(TODOS.IS_READ.eq((byte) 0))
                .and(TODOS.STATUS.ne("COMPLETED"))
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
}

