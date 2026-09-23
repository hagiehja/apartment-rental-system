package com.example.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.entity.NotificationMessage;

import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送通知消息
     * 
     * @param dto 发送消息DTO
     * @return 消息ID
     */
    Long sendNotification(SendNotificationDTO dto);

    /**
     * 查询用户的未读消息数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 查询用户的未读消息列表
     */
    List<NotificationMessage> getUnreadMessages(Long userId);

    /**
     * 分页查询用户的消息列表
     */
    IPage<NotificationMessage> getMessageList(Long userId, String type, Integer page, Integer size);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long messageId, Long userId);

    /**
     * 标记所有未读消息为已读
     */
    void markAllAsRead(Long userId);

    /**
     * 根据ID查询消息详情
     */
    NotificationMessage getMessageById(Long messageId, Long userId);

    /**
     * 删除通知消息
     */
    void deleteById(Long messageId);

    /**
     * 修复乱码的通知模板数据
     */
    void fixTemplateData();

    /**
     * 插入测试消息数据
     */
    void insertTestData();
}
