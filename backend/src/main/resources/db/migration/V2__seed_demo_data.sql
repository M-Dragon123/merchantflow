INSERT INTO sys_role (code, name) VALUES ('ADMIN', '管理员'), ('OPERATOR', '运营'), ('WAREHOUSE', '仓库员'), ('VIEWER', '只读成员');
INSERT INTO sys_user (username, password_hash, name) VALUES ('admin', '{noop}MerchantFlow@2026', '系统管理员');
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO product_category (name, sort) VALUES ('演示商品', 1);
INSERT INTO product_spu (category_id, name) VALUES (1, '商家管家演示商品');
INSERT INTO product_sku (spu_id, sku_code, specs_json, sale_price, cost_price) VALUES (1, 'MF-DEMO-001', JSON_OBJECT('规格', '标准版'), 99.00, 60.00);
INSERT INTO inventory (sku_id, available_qty, locked_qty, safety_stock) VALUES (1, 100, 0, 20);
