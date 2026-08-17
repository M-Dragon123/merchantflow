UPDATE sys_user SET password_hash = '$2b$12$JGM7zmwWNJDl4DqcZGc.9ukpRSoPI62IRYdRyJp6UIl8Mn31w.mWW' WHERE username = 'admin';
INSERT INTO sys_user (username, password_hash, name) VALUES
  ('operator', '$2b$12$JGM7zmwWNJDl4DqcZGc.9ukpRSoPI62IRYdRyJp6UIl8Mn31w.mWW', '演示运营'),
  ('warehouse', '$2b$12$JGM7zmwWNJDl4DqcZGc.9ukpRSoPI62IRYdRyJp6UIl8Mn31w.mWW', '演示仓库员'),
  ('viewer', '$2b$12$JGM7zmwWNJDl4DqcZGc.9ukpRSoPI62IRYdRyJp6UIl8Mn31w.mWW', '演示只读成员');
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2), (3, 3), (4, 4);
