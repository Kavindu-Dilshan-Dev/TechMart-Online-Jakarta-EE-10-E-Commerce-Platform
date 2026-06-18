-- =====================================================================
-- TechMart Online - MySQL 8 schema
-- Matches the JPA entity model in techmart-common. Run this first for the
-- optimized schema (FULLTEXT + composite indexes); the persistence unit uses
-- hibernate.hbm2ddl.auto=update, so it will reconcile any missing objects but
-- will not create these tuned indexes itself.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS techmart
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE techmart;

-- For a clean re-run, drop in dependency order.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS stock_alerts;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(60)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    salt          VARCHAR(64)  NOT NULL,
    first_name    VARCHAR(60),
    last_name     VARCHAR(60),
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(500),
    image_url   VARCHAR(255),
    parent_id   BIGINT,
    PRIMARY KEY (id),
    INDEX idx_categories_parent (parent_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE products (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    name             VARCHAR(150)  NOT NULL,
    description      VARCHAR(2000),
    sku              VARCHAR(60)   NOT NULL,
    price            DECIMAL(12,2) NOT NULL,
    discounted_price DECIMAL(12,2),
    brand            VARCHAR(80),
    image_url        VARCHAR(255),
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    version          BIGINT        NOT NULL DEFAULT 0,
    created_at       DATETIME      NOT NULL,
    category_id      BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    INDEX idx_products_category (category_id),
    INDEX idx_products_active (active),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    FULLTEXT INDEX idx_products_search (name, description)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE warehouses (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    name     VARCHAR(80) NOT NULL,
    location VARCHAR(120),
    address  VARCHAR(255),
    active   BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE inventory (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    product_id         BIGINT   NOT NULL,
    warehouse_id       BIGINT   NOT NULL,
    quantity_available INT      NOT NULL DEFAULT 0,
    quantity_reserved  INT      NOT NULL DEFAULT 0,
    reorder_threshold  INT      NOT NULL DEFAULT 10,
    reorder_quantity   INT      NOT NULL DEFAULT 50,
    last_updated       DATETIME,
    version            BIGINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_inventory_product_warehouse UNIQUE (product_id, warehouse_id),
    INDEX idx_inventory_product (product_id),
    INDEX idx_inventory_warehouse (warehouse_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    order_number     VARCHAR(40)   NOT NULL,
    user_id          BIGINT        NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    subtotal         DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax              DECIMAL(12,2) NOT NULL DEFAULT 0,
    shipping_cost    DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    shipping_address VARCHAR(500),
    order_date       DATETIME      NOT NULL,
    processed_at     DATETIME,
    shipped_at       DATETIME,
    delivered_at     DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    INDEX idx_orders_user_status (user_id, status),
    INDEX idx_orders_status (status),
    INDEX idx_orders_date (order_date),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE order_items (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    order_id              BIGINT        NOT NULL,
    product_id            BIGINT,
    quantity              INT           NOT NULL,
    unit_price            DECIMAL(12,2) NOT NULL,
    total_price           DECIMAL(12,2) NOT NULL,
    product_snapshot_name VARCHAR(150),
    product_snapshot_sku  VARCHAR(60),
    PRIMARY KEY (id),
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE payments (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    order_id               BIGINT        NOT NULL,
    payment_reference      VARCHAR(60)   NOT NULL,
    gateway_transaction_id VARCHAR(120),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    amount                 DECIMAL(12,2) NOT NULL,
    currency               VARCHAR(8)    NOT NULL DEFAULT 'LKR',
    payment_method         VARCHAR(40),
    initiated_at           DATETIME,
    completed_at           DATETIME,
    gateway_response       VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT uq_payments_reference UNIQUE (payment_reference),
    INDEX idx_payments_status (status),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(1000),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL,
    read_at    DATETIME,
    PRIMARY KEY (id),
    INDEX idx_notifications_user_unread (user_id, is_read),
    INDEX idx_notifications_created (created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE stock_alerts (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    product_id  BIGINT   NOT NULL,
    notified    BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL,
    notified_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uq_stock_alerts_user_product UNIQUE (user_id, product_id),
    INDEX idx_stock_alerts_product_notified (product_id, notified),
    CONSTRAINT fk_stock_alerts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_alerts_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    action       VARCHAR(80)  NOT NULL,
    entity_type  VARCHAR(60),
    entity_id    VARCHAR(60),
    performed_by VARCHAR(60),
    details      VARCHAR(1000),
    timestamp    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
CREATE TABLE user_sessions (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token      VARCHAR(80) NOT NULL,
    created_at DATETIME    NOT NULL,
    expires_at DATETIME    NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_sessions_token UNIQUE (token),
    INDEX idx_user_sessions_user (user_id),
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;
