package com.example.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板实体类
 */
@Data
@TableName("notification_template")
public class NotificationTemplate {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码(唯一)
     */
    private String code;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 消息类型(ORDER/PAYMENT/CONTRACT)
     */
    private String type;

    /**
     * 标题模板
     */
    private String titleTemplate;

    /**
     * 内容模板
     */
    private String contentTemplate;

    /**
     * 是否启用(0-禁用,1-启用)
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
