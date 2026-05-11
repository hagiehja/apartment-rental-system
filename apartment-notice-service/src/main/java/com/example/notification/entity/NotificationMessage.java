package com.example.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息实体类
 */
@Data
@TableName("notification_message")
public class NotificationMessage {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 消息类型(ORDER订单/PAYMENT支付/CONTRACT合同)
     */
    private String type;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 业务ID(订单ID/支付ID/合同ID)
     */
    private Long bizId;

    /**
     * 是否已读(0-未读,1-已读)
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 阅读时间
     */
    private LocalDateTime readAt;
}
