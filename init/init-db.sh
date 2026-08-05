#!/bin/bash
set -e

mysql -u root -p"$MYSQL_ROOT_PASSWORD" <<-EOSQL
    -- 1. WRITE DATABASE SETUP
    CREATE DATABASE IF NOT EXISTS ledger_write_db;
    USE ledger_write_db;

    CREATE TABLE IF NOT EXISTS ledger_events (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        aggregate_id VARCHAR(64) NOT NULL,
        version INT NOT NULL,
        even_type VARCHAR(100) NOT NULL,
        payload JSON NOT NULL,
        created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        trace_id VARCHAR(64) NOT NULL,
        CONSTRAINT uq_aggregate_version UNIQUE (aggregate_id, version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    CREATE TABLE IF NOT EXISTS account_snapshots (
        aggregate_id VARCHAR(64) PRIMARY KEY,
        version INT NOT NULL,
        balance DECIMAL(18,4) NOT NULL,
        updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    CREATE TABLE idempotency_keys (
        client_key VARCHAR(128) PRIMARY KEY,
        response_payload JSON NOT NULL,
        status_code INT NOT NULL,
        expires_at TIMESTAMP NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    SET GLOBAL event_scheduler=ON;
    CREATE EVENT IF NOT EXISTS purge_expired_idempotency_keys
    ON SCHEDULE EVERY 1 HOUR
    DO
       DELETE FROM idempotency_keys WHERE expires_at < NOW();
    -- 2. READ DATABASE SETUP
    CREATE DATABASE IF NOT EXISTS ledger_read_db;
    USE ledger_read_db;

    CREATE TABLE IF NOT EXISTS wallet_balances (
        account_id VARCHAR(64) PRIMARY KEY,
        balance DECIMAL(18,4) NOT NULL,
        last_event_version INT NOT NULL,
        last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    CREATE TABLE IF NOT EXISTS transaction_history (
        transaction_id VARCHAR(64) PRIMARY KEY,
        account_id VARCHAR(64) NOT NULL,
        type VARCHAR(20) NOT NULL,
        amount DECIMAL(18,4) NOT NULL,
        balance_after DECIMAL(18,4) NOT NULL,
        timestamp TIMESTAMP(6) NOT NULL,
        INDEX idx_account_timestamp (account_id, timestamp DESC)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- 3. DEBEZIUM REPLICATION USER
    CREATE USER IF NOT EXISTS 'debezium'@'%' IDENTIFIED BY '${DEBEZIUM_PASSWORD}';
    GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium'@'%';
    FLUSH PRIVILEGES;
EOSQL