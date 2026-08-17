-- V7：库存盘点（盘点单 + 明细，差异在完成时生成 ADJUSTMENT 库存流水）
CREATE TABLE stocktake (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stocktake_no VARCHAR(40) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  operator_name VARCHAR(64) NOT NULL,
  remark VARCHAR(255) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_stocktake_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stocktake_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stocktake_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  system_qty INT NOT NULL,
  counted_qty INT NOT NULL,
  diff_qty INT NOT NULL,
  CONSTRAINT fk_stocktake_item_stocktake FOREIGN KEY (stocktake_id) REFERENCES stocktake(id),
  CONSTRAINT fk_stocktake_item_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id),
  INDEX idx_stocktake_item_stocktake (stocktake_id),
  CONSTRAINT chk_stocktake_qty CHECK (system_qty >= 0 AND counted_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
