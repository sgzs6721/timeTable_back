package com.timetable.repository;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * jOOQ Repository基类
 */
public abstract class BaseRepository {
    
    @Autowired
    protected DSLContext dsl;
} 