-- V8：演示目录与历史订单（可重复导入的初始化数据）
-- 说明：所有金额/库存/流水保持一致性；订单创建时间与付款时间分布在过去 30 天内，用于演示工作台趋势与热销排行。
-- 实体 id 使用 100+ 高位，避免与运行期手工创建的数据（id 递增）冲突。

-- 1) 分类
INSERT INTO product_category (id, name, sort) VALUES
  (100, '服饰', 1),
  (101, '数码配件', 2),
  (102, '家居日用', 3);

-- 2) SPU / SKU
INSERT INTO product_spu (id, category_id, name) VALUES
  (100, 100, '纯棉基础T恤'),
  (101, 101, '无线蓝牙耳机'),
  (102, 102, '便携保温杯');

INSERT INTO product_sku (id, spu_id, sku_code, specs_json, sale_price, cost_price) VALUES
  (100, 100, 'MF-TEE-WHITE-M', JSON_OBJECT('颜色', '白色', '尺码', 'M'), 49.90, 25.00),
  (101, 101, 'MF-BUDS-X1', JSON_OBJECT('颜色', '曜石黑'), 199.00, 120.00),
  (102, 102, 'MF-CUP-500ML', JSON_OBJECT('容量', '500ml'), 89.00, 50.00);

-- 3) 库存（初始库存 - 历史已售 - 待付款锁定）
-- 销售汇总：T恤售出 9 件（含待发货/已发货/已完成），另有 1 件被待付款订单锁定；耳机售出 5 件；保温杯售出 4 件
INSERT INTO inventory (sku_id, available_qty, locked_qty, safety_stock) VALUES
  (100, 51, 1, 10),
  (101, 35, 0, 8),
  (102, 31, 0, 5);

-- 4) 库存流水（入库 + 历史订单扣减，保证可追溯）
INSERT INTO inventory_transaction (sku_id, transaction_type, quantity_delta, before_qty, after_qty, reason, operator_name, created_at) VALUES
  (100, 'INBOUND', 60, 0, 60, '初始化演示库存', 'system', DATE_SUB(NOW(), INTERVAL 30 DAY)),
  (101, 'INBOUND', 40, 0, 40, '初始化演示库存', 'system', DATE_SUB(NOW(), INTERVAL 30 DAY)),
  (102, 'INBOUND', 35, 0, 35, '初始化演示库存', 'system', DATE_SUB(NOW(), INTERVAL 30 DAY)),
  (100, 'OUTBOUND', -9, 60, 51, '演示历史订单扣减（V8 初始化）', 'system', DATE_SUB(NOW(), INTERVAL 29 DAY)),
  (101, 'OUTBOUND', -5, 40, 35, '演示历史订单扣减（V8 初始化）', 'system', DATE_SUB(NOW(), INTERVAL 29 DAY)),
  (102, 'OUTBOUND', -4, 35, 31, '演示历史订单扣减（V8 初始化）', 'system', DATE_SUB(NOW(), INTERVAL 29 DAY));

-- 5) 客户
INSERT INTO customer (id, name, mobile) VALUES
  (100, '王芳', '13800000001'),
  (101, '陈晨', '13800000002'),
  (102, '赵磊', '13800000003'),
  (103, '孙悦', '13800000004');

-- 6) 历史订单（近 30 天；状态覆盖已完成/已发货/待发货/待付款）
-- 单 1001：王芳 27 天前 已完成  T恤×2
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1001', 100, 'COMPLETED', 99.80, DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 100, 'MF-TEE-WHITE-M', 49.90, 2, 99.80);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 27 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 27 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 27 DAY)),
  (@oid, 'COMPLETE', 'SHIPPED', 'COMPLETED', '演示数据：订单已完成', 'system', DATE_SUB(NOW(), INTERVAL 26 DAY));

-- 单 1002：陈晨 24 天前 已完成  耳机×1
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1002', 101, 'COMPLETED', 199.00, DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 101, 'MF-BUDS-X1', 199.00, 1, 199.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 24 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 24 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 24 DAY)),
  (@oid, 'COMPLETE', 'SHIPPED', 'COMPLETED', '演示数据：订单已完成', 'system', DATE_SUB(NOW(), INTERVAL 23 DAY));

-- 单 1003：赵磊 21 天前 已完成  保温杯×1
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1003', 102, 'COMPLETED', 89.00, DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 102, 'MF-CUP-500ML', 89.00, 1, 89.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 21 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 21 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 21 DAY)),
  (@oid, 'COMPLETE', 'SHIPPED', 'COMPLETED', '演示数据：订单已完成', 'system', DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 单 1004：孙悦 17 天前 已完成  T恤×1 + 耳机×1
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1004', 103, 'COMPLETED', 248.90, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES
  (@oid, 100, 'MF-TEE-WHITE-M', 49.90, 1, 49.90),
  (@oid, 101, 'MF-BUDS-X1', 199.00, 1, 199.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 17 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 17 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 17 DAY)),
  (@oid, 'COMPLETE', 'SHIPPED', 'COMPLETED', '演示数据：订单已完成', 'system', DATE_SUB(NOW(), INTERVAL 16 DAY));

-- 单 1005：王芳 14 天前 已发货  T恤×3
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1005', 100, 'SHIPPED', 149.70, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 100, 'MF-TEE-WHITE-M', 49.90, 3, 149.70);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 14 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 14 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 14 DAY));

-- 单 1006：陈晨 11 天前 已完成  保温杯×2
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1006', 101, 'COMPLETED', 178.00, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 102, 'MF-CUP-500ML', 89.00, 2, 178.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 11 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 11 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 11 DAY)),
  (@oid, 'COMPLETE', 'SHIPPED', 'COMPLETED', '演示数据：订单已完成', 'system', DATE_SUB(NOW(), INTERVAL 10 DAY));

-- 单 1007：赵磊 8 天前 已完成  耳机×2
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1007', 102, 'COMPLETED', 398.00, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 101, 'MF-BUDS-X1', 199.00, 2, 398.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@oid, 'SHIP', 'PENDING_SHIPMENT', 'SHIPPED', '演示数据：订单已发货', 'system', DATE_SUB(NOW(), INTERVAL 8 DAY)),
  (@oid, 'COMPLETE', 'SHIPPED', 'COMPLETED', '演示数据：订单已完成', 'system', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- 单 1008：孙悦 5 天前 待发货  T恤×2 + 保温杯×1
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1008', 103, 'PENDING_SHIPMENT', 188.80, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES
  (@oid, 100, 'MF-TEE-WHITE-M', 49.90, 2, 99.80),
  (@oid, 102, 'MF-CUP-500ML', 89.00, 1, 89.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 单 1009：王芳 3 天前 待发货  耳机×1
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1009', 100, 'PENDING_SHIPMENT', 199.00, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 101, 'MF-BUDS-X1', 199.00, 1, 199.00);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (@oid, 'PAY', 'PENDING_PAYMENT', 'PENDING_SHIPMENT', '演示数据：确认付款', 'system', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 单 1010：陈晨 今天 待付款  T恤×1（已锁定库存，未付款）
INSERT INTO sales_order (order_no, customer_id, status, total_amount, paid_at, created_at) VALUES
  ('MF-DEMO-1010', 101, 'PENDING_PAYMENT', 49.90, NULL, NOW());
SET @oid = LAST_INSERT_ID();
INSERT INTO sales_order_item (order_id, sku_id, sku_name_snapshot, unit_price, quantity, subtotal_amount) VALUES (@oid, 100, 'MF-TEE-WHITE-M', 49.90, 1, 49.90);
INSERT INTO order_operation_log (order_id, action, from_status, to_status, remark, operator_name, created_at) VALUES
  (@oid, 'CREATE', NULL, 'PENDING_PAYMENT', '演示数据：创建订单', 'system', NOW());
