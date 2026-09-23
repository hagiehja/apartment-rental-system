-- 创建私信表
-- 执行时间：2026-02-05

USE apartment_db;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content VARCHAR(2000) NOT NULL COMMENT '消息内容',
    msg_type VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型: TEXT/IMAGE/ORDER/CONTRACT',
    related_id BIGINT COMMENT '关联ID（如订单ID/合同ID）',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_chat (sender_id, receiver_id)
) COMMENT '用户私信表';
