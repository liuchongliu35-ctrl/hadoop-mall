-- 秒杀系统数据库表创建脚本
CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET UTF8 COLLATE UTF8_GENERAL_CI;
USE seckill;

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role` TINYINT NOT NULL DEFAULT 2 COMMENT '角色：1-管理员，2-普通用户',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS `t_product_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID，0表示顶级分类',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `t_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_desc` TEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '商品库存',
    `category_id` BIGINT DEFAULT NULL COMMENT '商品分类ID',
    `img_url` VARCHAR(255) DEFAULT NULL COMMENT '商品图片URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态：1-上架，2-下架',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 秒杀活动表
CREATE TABLE IF NOT EXISTS `t_seckill_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `activity_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    `seckill_stock` INT NOT NULL COMMENT '秒杀库存',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '活动状态：0-未开始，1-进行中，2-已结束',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_activity` (`product_id`),
    KEY `idx_status` (`status`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS `seckill_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `product_id` BIGINT NOT NULL,
    `product_name` VARCHAR(128) DEFAULT NULL,
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    `quantity` INT NOT NULL DEFAULT 1,
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未支付,1-已支付,2-已取消',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_pay_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- 插入一些测试数据
-- 商品分类
INSERT INTO `t_product_category` (`category_name`, `parent_id`) VALUES 
('电子产品', 0),
('服装配饰', 0),
('家居用品', 0),
('手机数码', 1),
('电脑配件', 1),
('男装', 2),
('女装', 2);

-- 商品
INSERT INTO `t_product` (`product_name`, `product_desc`, `price`, `stock`, `category_id`, `img_url`, `status`) VALUES 
('iPhone 15 Pro', '苹果最新款手机，搭载A17 Pro芯片', 8999.00, 100, 4, 'https://example.com/iphone15.jpg', 1),
('华为Mate 60 Pro', '华为旗舰手机，支持卫星通话', 6999.00, 50, 4, 'https://example.com/mate60.jpg', 1),
('MacBook Pro 14', '苹果笔记本电脑，M3芯片', 14999.00, 30, 5, 'https://example.com/macbook.jpg', 1),
('商务男士衬衫', '高品质纯棉商务衬衫', 299.00, 200, 6, 'https://example.com/shirt.jpg', 1),
('时尚连衣裙', '夏季新款连衣裙，舒适透气', 199.00, 150, 7, 'https://example.com/dress.jpg', 1);

-- 用户
INSERT INTO `t_user` (`username`, `password`, `phone`, `email`, `role`) VALUES 
('admin', '$10$Lkhflx2E2DuNC3L6fj/E0e9Yhm2V3knSVc1wcIKM60YFcJoKNd.z.', '13800138000', 'admin@example.com', 1),
('testuser', '$10$Lkhflx2E2DuNC3L6fj/E0e9Yhm2V3knSVc1wcIKM60YFcJoKNd.z.', '13800138001', 'user@example.com', 2),
('user001', '$10$Lkhflx2E2DuNC3L6fj/E0e9Yhm2V3knSVc1wcIKM60YFcJoKNd.z.', '13800138002', 'user001@example.com', 2),
('user002', '$10$Lkhflx2E2DuNC3L6fj/E0e9Yhm2V3knSVc1wcIKM60YFcJoKNd.z.', '13800138003', 'user002@example.com', 2);

-- 注：密码都是 "123456" 的BCrypt加密结果

-- 秒杀活动（开始时间都晚于2025-12-20-01）
INSERT INTO `t_seckill_activity` (`activity_name`, `product_id`, `seckill_price`, `seckill_stock`, `start_time`, `end_time`, `status`) VALUES 
('iPhone 15 Pro 限时秒杀', 1, 6999.00, 10, '2025-12-20 10:00:00', '2025-12-20 12:00:00', 0),
('华为Mate 60 Pro 特惠活动', 2, 5999.00, 15, '2025-12-20 14:00:00', '2025-12-20 16:00:00', 0),
('MacBook Pro 14 闪购', 3, 12999.00, 5, '2025-12-20 20:00:00', '2025-12-20 22:00:00', 0),
('商务衬衫秒杀', 4, 199.00, 50, '2025-12-21 09:00:00', '2025-12-21 11:00:00', 0),
('时尚连衣裙特惠', 5, 99.00, 30, '2025-12-21 15:00:00', '2025-12-21 17:00:00', 0);

-- 秒杀订单测试数据
INSERT INTO `seckill_order` (`user_id`, `activity_id`, `product_id`, `product_name`, `order_no`, `seckill_price`, `quantity`, `total_amount`, `status`, `pay_time`) VALUES 
-- 已支付的订单
(2, 1, 1, 'iPhone 15 Pro', 'SK2025122010001', 6999.00, 1, 6999.00, 1, '2025-12-20 10:30:00'),
(3, 1, 1, 'iPhone 15 Pro', 'SK2025122010002', 6999.00, 1, 6999.00, 1, '2025-12-20 10:35:00'),
(2, 2, 2, '华为Mate 60 Pro', 'SK2025122014001', 5999.00, 1, 5999.00, 1, '2025-12-20 14:20:00'),
(4, 2, 2, '华为Mate 60 Pro', 'SK2025122014002', 5999.00, 2, 11998.00, 1, '2025-12-20 14:25:00'),
-- 未支付的订单
(3, 3, 3, 'MacBook Pro 14', 'SK2025122020001', 12999.00, 1, 12999.00, 0, NULL),
(4, 4, 4, '商务男士衬衫', 'SK2025122109001', 199.00, 3, 597.00, 0, NULL),
-- 已取消的订单
(2, 5, 5, '时尚连衣裙', 'SK2025122115001', 99.00, 2, 198.00, 2, NULL);