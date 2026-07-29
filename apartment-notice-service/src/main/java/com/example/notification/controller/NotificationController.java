package com.example.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.common.api.Result;
import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.entity.NotificationMessage;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务API接口
 */
@RestController
@RequestMapping("/notification")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 发送通知消息(内部服务调用)
     */
    @PostMapping("/send")
    public Result<Long> sendNotification(@RequestBody SendNotificationDTO dto) {
        try {
            Long messageId = notificationService.sendNotification(dto);
            return Result.success(messageId);
        } catch (Exception e) {
            log.error("发送通知失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询未读消息数量
     */
    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        try {
            Long count = notificationService.getUnreadCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("查询未读消息数量失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询未读消息列表
     */
    @GetMapping("/unread")
    public Result<List<NotificationMessage>> getUnreadMessages(@RequestParam Long userId) {
        try {
            List<NotificationMessage> messages = notificationService.getUnreadMessages(userId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("查询未读消息列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询消息列表
     */
    @GetMapping("/list")
    public Result<IPage<NotificationMessage>> getMessageList(
            @RequestParam Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<NotificationMessage> result = notificationService.getMessageList(
                    userId, type, page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询消息列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询消息详情
     */
    @GetMapping("/{messageId}")
    public Result<NotificationMessage> getMessageDetail(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        try {
            NotificationMessage message = notificationService.getMessageById(messageId, userId);
            return Result.success(message);
        } catch (Exception e) {
            log.error("查询消息详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/read/{messageId}")
    public Result<Void> markAsRead(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        try {
            notificationService.markAsRead(messageId, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记所有消息为已读
     */
    @PostMapping("/read/all")
    public Result<Void> markAllAsRead(@RequestParam Long userId) {
        try {
            notificationService.markAllAsRead(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("标记所有消息已读失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除消息（内部服务调用 / 管理用途）
     */
    @DeleteMapping("/{messageId}")
    public Result<Void> deleteNotification(@PathVariable Long messageId) {
        try {
            notificationService.deleteById(messageId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除通知失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "apartment-notice-service");
        data.put("status", "UP");
        data.put("port", 8091);
        return Result.success(data);
    }

    /**
     * 插入测试消息数据
     */
    @PostMapping("/test/insert")
    public Result<String> insertTestData() {
        try {
            notificationService.insertTestData();
            return Result.success("测试数据插入成功");
        } catch (Exception e) {
            log.error("插入测试数据失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修复通知模板数据（一次性维护接口）
     * 解决模板数据因字符集问题导致的乱码
     */
    @PostMapping("/fix-templates")
    public Result<String> fixTemplates() {
        try {
            notificationService.fixTemplateData();
            return Result.success("模板数据修复完成");
        } catch (Exception e) {
            log.error("修复模板数据失败", e);
            return Result.error(e.getMessage());
        }
    }
}
