-- ============================================================
-- I-Bot Intelligent Invoice Processing System
-- MySQL Schema v1.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS ibot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ibot_db;

-- ── Users ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    department      VARCHAR(100),
    role            VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    avatar_url      VARCHAR(500),
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- ── Vendors ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vendors (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    vendor_code     VARCHAR(50) UNIQUE,
    address         VARCHAR(200),
    email           VARCHAR(100),
    phone           VARCHAR(20),
    gst_number      VARCHAR(20),
    pan_number      VARCHAR(10),
    bank_account    VARCHAR(20),
    bank_ifsc       VARCHAR(15),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    total_invoices  INT NOT NULL DEFAULT 0,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_vendor_name (name)
) ENGINE=InnoDB;

-- ── Invoices ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS invoices (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number      VARCHAR(100),
    vendor_name         VARCHAR(200),
    invoice_date        DATE,
    due_date            DATE,
    subtotal            DECIMAL(15,2),
    gst_amount          DECIMAL(15,2),
    gst_rate            DECIMAL(5,2),
    total_amount        DECIMAL(15,2),
    currency            VARCHAR(5) DEFAULT 'INR',
    po_number           VARCHAR(100),
    payment_terms       VARCHAR(50),
    status              ENUM('PENDING','PROCESSING','EXTRACTED','VALIDATED','APPROVED','REJECTED','FLAGGED','DUPLICATE') NOT NULL DEFAULT 'PENDING',
    file_name           VARCHAR(255),
    file_path           VARCHAR(500),
    file_type           VARCHAR(10),
    file_size           BIGINT,
    ocr_confidence      DECIMAL(5,2),
    ocr_raw_text        LONGTEXT,
    is_duplicate        BOOLEAN NOT NULL DEFAULT FALSE,
    duplicate_of_id     BIGINT,
    notes               TEXT,
    validated_by        BIGINT,
    validated_at        DATETIME(6),
    vendor_id           BIGINT,
    uploaded_by         BIGINT NOT NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (vendor_id)   REFERENCES vendors(id) ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by) REFERENCES users(id)   ON DELETE RESTRICT,
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_status         (status),
    INDEX idx_invoice_date   (invoice_date),
    INDEX idx_vendor_id      (vendor_id),
    INDEX idx_uploaded_by    (uploaded_by)
) ENGINE=InnoDB;

-- ── Invoice Line Items ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS invoice_line_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT NOT NULL,
    description     VARCHAR(500),
    quantity        DECIMAL(10,2),
    unit_price      DECIMAL(15,2),
    total_price     DECIMAL(15,2),
    hsn_sac_code    VARCHAR(20),
    gst_rate        DECIMAL(5,2),
    line_number     INT,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_invoice_id (invoice_id)
) ENGINE=InnoDB;

-- ── Audit Logs ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT,
    action          VARCHAR(50) NOT NULL,
    old_value       TEXT,
    new_value       TEXT,
    user_id         BIGINT,
    user_name       VARCHAR(100),
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),
    description     VARCHAR(1000),
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_entity    (entity_type, entity_id),
    INDEX idx_audit_user      (user_id),
    INDEX idx_audit_timestamp (created_at)
) ENGINE=InnoDB;
