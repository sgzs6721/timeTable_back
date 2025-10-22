package com.timetable.repository;

import com.timetable.entity.CustomerStatusHistory;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;

@Repository
public class CustomerStatusHistoryRepository extends BaseRepository {

    @Autowired
    private DSLContext dsl;

    public CustomerStatusHistory save(CustomerStatusHistory history) {
        try {
            dsl.insertInto(table("customer_status_history"))
                    .set(field("customer_id"), history.getCustomerId())
                    .set(field("from_status"), history.getFromStatus())
                    .set(field("to_status"), history.getToStatus())
                    .set(field("notes"), history.getNotes())
                    .set(field("created_by"), history.getCreatedBy())
                    .set(field("created_at"), LocalDateTime.now())
                    .execute();

            // 获取最后插入的ID
            Long id = dsl.select(field("LAST_INSERT_ID()", Long.class))
                    .fetchOne()
                    .value1();

            return findById(id);
        } catch (Exception e) {
            throw new RuntimeException("保存状态历史记录失败: " + e.getMessage(), e);
        }
    }

    public CustomerStatusHistory findById(Long id) {
        return dsl.select()
                .from(table("customer_status_history"))
                .where(field("id").eq(id))
                .fetchOne(record -> {
                    CustomerStatusHistory history = new CustomerStatusHistory();
                    history.setId(record.get(field("id", Long.class)));
                    history.setCustomerId(record.get(field("customer_id", Long.class)));
                    history.setFromStatus(record.get(field("from_status", String.class)));
                    history.setToStatus(record.get(field("to_status", String.class)));
                    history.setNotes(record.get(field("notes", String.class)));
                    history.setCreatedBy(record.get(field("created_by", Long.class)));
                    history.setCreatedAt(record.get(field("created_at", LocalDateTime.class)));
                    return history;
                });
    }

    public List<CustomerStatusHistory> findByCustomerId(Long customerId) {
        return dsl.select()
                .from(table("customer_status_history"))
                .where(field("customer_id").eq(customerId))
                .orderBy(field("created_at").desc())
                .fetch(record -> {
                    CustomerStatusHistory history = new CustomerStatusHistory();
                    history.setId(record.get(field("id", Long.class)));
                    history.setCustomerId(record.get(field("customer_id", Long.class)));
                    history.setFromStatus(record.get(field("from_status", String.class)));
                    history.setToStatus(record.get(field("to_status", String.class)));
                    history.setNotes(record.get(field("notes", String.class)));
                    history.setCreatedBy(record.get(field("created_by", Long.class)));
                    history.setCreatedAt(record.get(field("created_at", LocalDateTime.class)));
                    return history;
                });
    }
}

