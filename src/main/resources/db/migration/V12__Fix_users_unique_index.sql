SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'users'
     AND INDEX_NAME = 'idx_users_username'
     AND NON_UNIQUE = 1) > 0,
    'ALTER TABLE users DROP INDEX idx_users_username',
    'SELECT "Username index does not exist"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


CREATE UNIQUE INDEX idx_users_username_deleted_status_id ON users (username, is_deleted, status, id);

