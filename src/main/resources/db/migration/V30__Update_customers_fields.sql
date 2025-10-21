-- 修改客户表字段
ALTER TABLE customers 
  MODIFY COLUMN parent_phone VARCHAR(20) NULL COMMENT '家长电话',
  MODIFY COLUMN parent_relation ENUM('MOTHER', 'FATHER', 'OTHER') NULL COMMENT '家长关系：妈妈/爸爸/其他',
  ADD COLUMN child_gender ENUM('MALE', 'FEMALE') NULL COMMENT '孩子性别：男/女' AFTER child_name,
  ADD COLUMN child_age INT NULL COMMENT '孩子年龄' AFTER child_gender,
  ADD COLUMN wechat VARCHAR(100) NULL COMMENT '微信号' AFTER parent_phone,
  MODIFY COLUMN source VARCHAR(200) NULL COMMENT '推广渠道';

