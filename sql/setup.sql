-- ============================================
-- Simple Online Charging System - DB Setup
-- Run this on your NeonDB PostgreSQL instance
-- ============================================

CREATE TABLE IF NOT EXISTS customers (
    msisdn  VARCHAR(20)    PRIMARY KEY,
    balance DECIMAL(12,2)  NOT NULL
);

-- Sample data
INSERT INTO customers (msisdn, balance) VALUES ('01000202', 55.00)
    ON CONFLICT (msisdn) DO NOTHING;
INSERT INTO customers (msisdn, balance) VALUES ('01001111', 10.00)
    ON CONFLICT (msisdn) DO NOTHING;
INSERT INTO customers (msisdn, balance) VALUES ('01002222', 2.50)
    ON CONFLICT (msisdn) DO NOTHING;
