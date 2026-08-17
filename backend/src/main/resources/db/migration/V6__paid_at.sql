-- V6：工作台与订单查询支持
-- 1) 订单增加付款时间：销售额按付款时间口径统计（排除已取消/已退款）
ALTER TABLE sales_order
  ADD COLUMN paid_at DATETIME NULL AFTER total_amount;
CREATE INDEX idx_sales_order_paid_at ON sales_order (paid_at);
