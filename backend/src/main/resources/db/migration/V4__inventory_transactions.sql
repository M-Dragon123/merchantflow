CREATE TABLE inventory_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_id BIGINT NOT NULL,
  transaction_type VARCHAR(32) NOT NULL,
  quantity_delta INT NOT NULL,
  before_qty INT NOT NULL,
  after_qty INT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  operator_name VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_inventory_transaction_sku_created (sku_id, created_at),
  CONSTRAINT fk_inventory_transaction_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO inventory_transaction (sku_id, transaction_type, quantity_delta, before_qty, after_qty, reason, operator_name)
VALUES (1, 'INBOUND', 100, 0, 100, '初始化演示库存', 'system');
