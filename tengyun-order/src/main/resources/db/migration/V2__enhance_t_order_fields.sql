SET @col_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_order' AND column_name = 'request_id'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE t_order ADD COLUMN request_id VARCHAR(64) NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_order' AND column_name = 'quantity'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE t_order ADD COLUMN quantity INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_order' AND column_name = 'status'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE t_order ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''CREATED''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 't_order' AND column_name = 'total_amount'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE t_order ADD COLUMN total_amount DECIMAL(10,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE t_order
    MODIFY COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP;

UPDATE t_order
SET total_amount = price * quantity
WHERE total_amount IS NULL AND price IS NOT NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_order'
      AND index_name = 'uk_request_id'
);
SET @idx_sql := IF(@idx_exists = 0, 'ALTER TABLE t_order ADD UNIQUE INDEX uk_request_id (request_id)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;
