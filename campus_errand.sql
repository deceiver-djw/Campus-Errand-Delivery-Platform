-- ============================================================
-- 校园跑腿代取快递平台 — 数据库设计
-- 数据库名称: campus_errand
-- 字符集: utf8mb4
-- 基于: SpringBoot + MyBatisPlus + MySQL 8.0
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `campus_errand`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `campus_errand`;

-- ============================================================
-- 1. 用户表 (user)
-- 说明: 存储所有注册用户信息，包含普通用户、跑腿员、管理员
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `student_no`    VARCHAR(20)  NOT NULL COMMENT '学号（唯一标识）',
    `nickname`      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `password`      VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`        VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `real_name`     VARCHAR(20)  DEFAULT NULL COMMENT '真实姓名',
    `dormitory`     VARCHAR(100) DEFAULT NULL COMMENT '宿舍楼栋（默认送达地址）',
    `balance`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    `role`          TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0=普通用户, 1=跑腿员, 2=管理员',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    `is_verified`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否实名认证: 0=未认证, 1=已认证',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_phone` (`phone`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- ============================================================
-- 2. 快递点表 (express_point)
-- 说明: 校内及周边常见快递点列表，可配置化管理
-- ============================================================
DROP TABLE IF EXISTS `express_point`;
CREATE TABLE `express_point` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '快递点ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '快递点名称（如: 菜鸟驿站、京东快递、顺丰速运）',
    `location`    VARCHAR(200) DEFAULT NULL COMMENT '位置描述',
    `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序权重（越小越靠前）',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快递点表';



-- ============================================================
-- 3. 跑腿员表 (runner)
-- 说明: 存储跑腿员申请信息、审核状态、评价统计
-- ============================================================
DROP TABLE IF EXISTS `runner`;
CREATE TABLE `runner` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '跑腿员ID',
    `user_id`           BIGINT       NOT NULL COMMENT '关联用户ID',
    `real_name`         VARCHAR(20)  NOT NULL COMMENT '真实姓名',
    `id_card`           VARCHAR(18)  NOT NULL COMMENT '身份证号',
    `phone`             VARCHAR(20)  NOT NULL COMMENT '手机号',
    `student_card_img`  VARCHAR(500) DEFAULT NULL COMMENT '学生证/身份证照片URL',
    `audit_status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态: 0=待审核, 1=已通过, 2=已拒绝',
    `audit_remark`      VARCHAR(500) DEFAULT NULL COMMENT '审核备注（拒绝原因）',
    `audit_time`        DATETIME     DEFAULT NULL COMMENT '审核时间',
    `score`             DECIMAL(3,2) NOT NULL DEFAULT 5.00 COMMENT '平均评分（1.00~5.00）',
    `order_count`       INT          NOT NULL DEFAULT 0 COMMENT '累计接单数',
    `total_income`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '累计收益',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_score` (`score`),
    KEY `idx_order_count` (`order_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跑腿员表';


-- ============================================================
-- 4. 任务表 (task)
-- 说明: 用户发布的代取快递任务
-- ============================================================
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `user_id`        BIGINT         NOT NULL COMMENT '发布用户ID',
    `express_point`  VARCHAR(100)   NOT NULL COMMENT '快递点名称',
    `pickup_code`    VARCHAR(100)   NOT NULL COMMENT '取件码',
    `pickup_img`     VARCHAR(500)   DEFAULT NULL COMMENT '取件码截图URL',
    `delivery_addr`  VARCHAR(200)   NOT NULL COMMENT '送达地址',
    `fee`            DECIMAL(10,2)  NOT NULL COMMENT '跑腿费',
    `is_urgent`      TINYINT        NOT NULL DEFAULT 0 COMMENT '是否加急: 0=普通, 1=加急',
    `remark`         VARCHAR(500)   DEFAULT NULL COMMENT '备注说明',
    `status`         TINYINT        NOT NULL DEFAULT 0 COMMENT '任务状态: 0=待接单, 1=已接单, 2=已取消',
    `version`        INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（用于抢单并发控制）',
    `cancel_time`    DATETIME       DEFAULT NULL COMMENT '取消时间',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_express_point` (`express_point`),
    KEY `idx_fee` (`fee`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';


-- ============================================================
-- 5. 订单表 (order)
-- 说明: 跑腿员抢单后生成的订单，跟踪全流程
--       状态码: 0=已接单, 1=已取件, 2=配送中, 3=已送达, 4=待评价, 5=已完成
-- ============================================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `task_id`         BIGINT      NOT NULL COMMENT '关联任务ID',
    `runner_id`       BIGINT      NOT NULL COMMENT '跑腿员ID（关联runner表）',
    `user_id`         BIGINT      NOT NULL COMMENT '发布用户ID（冗余，便于查询）',
    `status`          TINYINT     NOT NULL DEFAULT 0 COMMENT '订单状态: 0=已接单, 1=已取件, 2=配送中, 3=已送达, 4=待评价, 5=已完成',
    `accept_time`     DATETIME    NOT NULL COMMENT '接单时间',
    `pickup_time`     DATETIME    DEFAULT NULL COMMENT '取件时间',
    `delivery_time`   DATETIME    DEFAULT NULL COMMENT '配送开始时间',
    `arrive_time`     DATETIME    DEFAULT NULL COMMENT '送达时间',
    `complete_time`   DATETIME    DEFAULT NULL COMMENT '完成时间（双方互评后）',
    `user_confirm`    TINYINT     NOT NULL DEFAULT 0 COMMENT '用户是否确认收货: 0=未确认, 1=已确认',
    `version`         INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_runner_id` (`runner_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_accept_time` (`accept_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';


-- ============================================================
-- 6. 订单状态日志表 (order_log)
-- 说明: 记录订单每次状态变更的详细日志
-- ============================================================
DROP TABLE IF EXISTS `order_log`;
CREATE TABLE `order_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id`     BIGINT       NOT NULL COMMENT '关联订单ID',
    `from_status`  TINYINT      DEFAULT NULL COMMENT '变更前状态',
    `to_status`    TINYINT      NOT NULL COMMENT '变更后状态',
    `operator_id`  BIGINT       NOT NULL COMMENT '操作人ID（用户ID）',
    `operator_role` TINYINT     NOT NULL DEFAULT 0 COMMENT '操作人角色: 0=用户, 1=跑腿员, 2=管理员',
    `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态日志表';


-- ============================================================
-- 7. 评价表 (evaluation)
-- 说明: 用户和跑腿员双方互评记录
--       type字段: 0=用户评跑腿员, 1=跑腿员评用户
-- ============================================================
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`     BIGINT       NOT NULL COMMENT '关联订单ID',
    `from_user_id` BIGINT       NOT NULL COMMENT '评价人ID',
    `to_user_id`   BIGINT       NOT NULL COMMENT '被评人ID',
    `score`        TINYINT      NOT NULL COMMENT '评分（1~5星）',
    `content`      VARCHAR(500) DEFAULT NULL COMMENT '文字评价内容',
    `tags`         VARCHAR(200) DEFAULT NULL COMMENT '评价标签（逗号分隔，如: "服务好,速度快"）',
    `type`         TINYINT      NOT NULL COMMENT '评价类型: 0=用户评跑腿员, 1=跑腿员评用户',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_from_type` (`order_id`, `from_user_id`, `type`),
    KEY `idx_from_user_id` (`from_user_id`),
    KEY `idx_to_user_id` (`to_user_id`),
    KEY `idx_score` (`score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';


-- ============================================================
-- 8. 结算表 (settlement)
-- 说明: 任务完成后的结算记录
-- ============================================================
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '结算ID',
    `order_id`    BIGINT         NOT NULL COMMENT '关联订单ID',
    `runner_id`   BIGINT         NOT NULL COMMENT '跑腿员ID',
    `user_id`     BIGINT         NOT NULL COMMENT '支付用户ID（冗余）',
    `amount`      DECIMAL(10,2)  NOT NULL COMMENT '结算金额（跑腿费）',
    `status`      TINYINT        NOT NULL DEFAULT 0 COMMENT '结算状态: 0=待结算, 1=已结算, 2=已退款',
    `settle_time` DATETIME       DEFAULT NULL COMMENT '结算时间',
    `remark`      VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_runner_id` (`runner_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_settle_time` (`settle_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算表';


-- ============================================================
-- 9. 消息表 (message)
-- 说明: 站内私信/消息沟通记录
-- ============================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `from_user_id` BIGINT       NOT NULL COMMENT '发送人ID',
    `to_user_id`   BIGINT       NOT NULL COMMENT '接收人ID',
    `content`      TEXT         NOT NULL COMMENT '消息内容',
    `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    `read_time`    DATETIME     DEFAULT NULL COMMENT '阅读时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_to` (`from_user_id`, `to_user_id`),
    KEY `idx_to_read` (`to_user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';


-- ============================================================
-- 10. 通知表 (notification)
-- 说明: 系统自动推送的通知消息
-- ============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
    `type`        VARCHAR(50)  NOT NULL COMMENT '通知类型: TASK_ASSIGNED/STATUS_CHANGED/SETTLEMENT/ANNOUNCEMENT 等',
    `title`       VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content`     TEXT         NOT NULL COMMENT '通知内容',
    `related_id`  BIGINT       DEFAULT NULL COMMENT '关联业务ID（任务ID/订单ID等）',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    `read_time`   DATETIME     DEFAULT NULL COMMENT '阅读时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_type` (`type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';


-- ============================================================
-- 11. 平台资金流水表 (transaction_record)
-- 说明: 平台资金流转明细，用于结算管理和对账
-- ============================================================
DROP TABLE IF EXISTS `transaction_record`;
CREATE TABLE `transaction_record` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `user_id`     BIGINT         NOT NULL COMMENT '用户ID',
    `type`        TINYINT        NOT NULL COMMENT '交易类型: 0=充值, 1=支付跑腿费, 2=收入跑腿费, 3=提现, 4=退款',
    `amount`      DECIMAL(10,2)  NOT NULL COMMENT '交易金额',
    `balance`     DECIMAL(10,2)  NOT NULL COMMENT '交易后余额',
    `related_id`  BIGINT         DEFAULT NULL COMMENT '关联业务ID（订单ID/结算ID等）',
    `remark`      VARCHAR(500)   DEFAULT NULL COMMENT '备注说明',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台资金流水表';


-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO `user` (`student_no`, `nickname`, `password`, `phone`, `real_name`, `role`, `status`, `is_verified`) VALUES
('admin001', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000000', '管理员', 2, 1, 1);

-- 初始化快递点数据
INSERT INTO `express_point` (`name`, `location`, `sort_order`) VALUES
('菜鸟驿站', '校内学生活动中心一楼', 1),
('京东快递', '校内商业街京东派', 2),
('顺丰速运', '校内行政楼对面', 3),
('韵达快递', '校门口快递柜', 4),
('中通快递', '校内体育馆旁', 5),
('圆通快递', '校外美食街旁', 6),
('申通快递', '校北门快递点', 7),
('邮政EMS', '校内邮局', 8),
('极兔速递', '校门口快递超市', 9),
('百世快递', '校内超市二楼', 10);
