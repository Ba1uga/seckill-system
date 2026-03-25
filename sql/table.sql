CREATE TABLE t_user (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                        username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                        password VARCHAR(100) NOT NULL COMMENT '密码（加密）',
                        phone VARCHAR(20) UNIQUE COMMENT '手机号',
                        status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='用户表';

CREATE TABLE t_product (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
                           name VARCHAR(100) NOT NULL COMMENT '商品名称',
                           description TEXT COMMENT '商品描述',
                           price DECIMAL(10,2) NOT NULL COMMENT '原价',
                           stock INT NOT NULL COMMENT '库存',
                           status TINYINT DEFAULT 1 COMMENT '1上架 0下架',
                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='商品表';

CREATE TABLE t_seckill_product (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   product_id BIGINT NOT NULL COMMENT '商品ID',
                                   seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
                                   stock INT NOT NULL COMMENT '秒杀库存',
                                   start_time DATETIME NOT NULL,
                                   end_time DATETIME NOT NULL,
                                   status TINYINT DEFAULT 1 COMMENT '1开启 0关闭',
                                   version INT DEFAULT 0 COMMENT '乐观锁版本号',
                                   create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                   UNIQUE KEY uk_product_id (product_id)
) COMMENT='秒杀商品表';

CREATE TABLE t_order (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         total_amount DECIMAL(10,2) NOT NULL,
                         status TINYINT DEFAULT 0 COMMENT '0未支付 1已支付 2已取消',
                         create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                         update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='订单表';

CREATE TABLE t_seckill_order (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 user_id BIGINT NOT NULL,
                                 seckill_product_id BIGINT NOT NULL,
                                 order_id BIGINT NOT NULL,
                                 create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

                                 UNIQUE KEY uk_user_product (user_id, seckill_product_id)
) COMMENT='秒杀订单表（防重复）';