package com.example.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.notification.dto.SendNotificationDTO;
import com.example.notification.entity.NotificationMessage;
import com.example.notification.entity.NotificationTemplate;
import com.example.notification.mapper.NotificationMessageMapper;
import com.example.notification.mapper.NotificationTemplateMapper;
import com.example.notification.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知服务业务逻辑实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMessageMapper messageMapper;
    private final NotificationTemplateMapper templateMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 发送通知消息
     * 
     * @param dto 发送消息DTO
     * @return 消息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendNotification(SendNotificationDTO dto) {
        log.info("开始发送通知: userId={}, templateCode={}", dto.getUserId(), dto.getTemplateCode());

        // 1. 查询消息模板
        LambdaQueryWrapper<NotificationTemplate> templateQuery = new LambdaQueryWrapper<>();
        templateQuery.eq(NotificationTemplate::getCode, dto.getTemplateCode())
                .eq(NotificationTemplate::getEnabled, 1);

        NotificationTemplate template = templateMapper.selectOne(templateQuery);
        if (template == null) {
            throw new RuntimeException("消息模板不存在或已禁用: " + dto.getTemplateCode());
        }

        // 2. 替换模板变量
        String title = replaceTemplate(template.getTitleTemplate(), dto.getParams());
        String content = replaceTemplate(template.getContentTemplate(), dto.getParams());

        // 3. 创建消息记录
        NotificationMessage message = new NotificationMessage();
        message.setUserId(dto.getUserId());
        message.setType(template.getType());
        message.setTitle(title);
        message.setContent(content);
        message.setBizId(dto.getBizId());
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());

        messageMapper.insert(message);

        log.info("通知发送成功: messageId={}, userId={}, title={}",
                message.getId(), dto.getUserId(), title);

        return message.getId();
    }

    /**
     * 替换模板中的变量
     * 支持 {{variableName}} 格式的变量替换
     */
    private String replaceTemplate(String template, Map<String, String> params) {
        if (template == null || params == null) {
            return template;
        }

        // 匹配 {{variableName}} 格式的变量
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(template);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = params.getOrDefault(varName, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 查询用户的未读消息数量
     */
    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<NotificationMessage> query = new LambdaQueryWrapper<>();
        query.eq(NotificationMessage::getUserId, userId)
                .eq(NotificationMessage::getIsRead, 0);

        return messageMapper.selectCount(query);
    }

    /**
     * 查询用户的未读消息列表
     */
    @Override
    public List<NotificationMessage> getUnreadMessages(Long userId) {
        LambdaQueryWrapper<NotificationMessage> query = new LambdaQueryWrapper<>();
        query.eq(NotificationMessage::getUserId, userId)
                .eq(NotificationMessage::getIsRead, 0)
                .orderByDesc(NotificationMessage::getCreatedAt);

        return messageMapper.selectList(query);
    }

    /**
     * 分页查询用户的消息列表
     */
    @Override
    public IPage<NotificationMessage> getMessageList(
            Long userId,
            String type,
            Integer page,
            Integer size) {

        Page<NotificationMessage> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<NotificationMessage> query = new LambdaQueryWrapper<>();
        query.eq(NotificationMessage::getUserId, userId);

        // 如果指定了类型,则按类型过滤
        if (type != null && !type.isEmpty()) {
            query.eq(NotificationMessage::getType, type);
        }

        query.orderByDesc(NotificationMessage::getCreatedAt);

        return messageMapper.selectPage(pageParam, query);
    }

    /**
     * 标记消息为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId, Long userId) {
        // 验证消息是否属于该用户
        NotificationMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        if (!message.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该消息");
        }

        if (message.getIsRead() == 1) {
            log.info("消息已是已读状态: messageId={}", messageId);
            return;
        }

        // 更新为已读
        LambdaUpdateWrapper<NotificationMessage> update = new LambdaUpdateWrapper<>();
        update.eq(NotificationMessage::getId, messageId)
                .set(NotificationMessage::getIsRead, 1)
                .set(NotificationMessage::getReadAt, LocalDateTime.now());

        messageMapper.update(null, update);

        log.info("消息已标记为已读: messageId={}", messageId);
    }

    /**
     * 标记所有未读消息为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<NotificationMessage> update = new LambdaUpdateWrapper<>();
        update.eq(NotificationMessage::getUserId, userId)
                .eq(NotificationMessage::getIsRead, 0)
                .set(NotificationMessage::getIsRead, 1)
                .set(NotificationMessage::getReadAt, LocalDateTime.now());

        messageMapper.update(null, update);

        log.info("所有未读消息已标记为已读: userId={}", userId);
    }

    /**
     * 根据ID查询消息详情
     */
    @Override
    public NotificationMessage getMessageById(Long messageId, Long userId) {
        NotificationMessage message = messageMapper.selectById(messageId);

        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        if (!message.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看该消息");
        }

        return message;
    }

    /**
     * 插入测试消息数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertTestData() {
        // 先检查是否已有消息数据
        Long count = messageMapper.selectCount(null);
        if (count > 0) {
            log.info("已有消息数据，跳过插入测试数据");
            return;
        }

        // 插入测试消息数据
        LocalDateTime now = LocalDateTime.now();

        NotificationMessage msg1 = new NotificationMessage();
        msg1.setUserId(1L);
        msg1.setType("ORDER");
        msg1.setTitle("您收到了一个新的订单");
        msg1.setContent("您的房源【精装两室一厅】收到了新订单，订单号：ORD20260214001，租期：2026-03-01 至 2026-05-31，租金：¥3000元/月。请及时处理。");
        msg1.setBizId(1L);
        msg1.setIsRead(0);
        msg1.setCreatedAt(now);
        messageMapper.insert(msg1);

        NotificationMessage msg2 = new NotificationMessage();
        msg2.setUserId(1L);
        msg2.setType("PAYMENT");
        msg2.setTitle("支付成功");
        msg2.setContent("您的订单ORD20260214001支付成功，支付金额：¥9000元，支付方式：钱包支付。");
        msg2.setBizId(1L);
        msg2.setIsRead(0);
        msg2.setCreatedAt(now);
        messageMapper.insert(msg2);

        NotificationMessage msg3 = new NotificationMessage();
        msg3.setUserId(1L);
        msg3.setType("CONTRACT");
        msg3.setTitle("租赁合同已生成");
        msg3.setContent("订单ORD20260214001的租赁合同已生成（合同编号：CON20260214001），请及时查看并签署。");
        msg3.setBizId(1L);
        msg3.setIsRead(0);
        msg3.setCreatedAt(now);
        messageMapper.insert(msg3);

        NotificationMessage msg4 = new NotificationMessage();
        msg4.setUserId(2L);
        msg4.setType("ORDER");
        msg4.setTitle("订单支付成功通知");
        msg4.setContent("订单ORD20260214002已成功支付，房源：【温馨单间】，租期：2026-03-01 至 2026-06-30，租金：¥1500元/月。");
        msg4.setBizId(2L);
        msg4.setIsRead(0);
        msg4.setCreatedAt(now);
        messageMapper.insert(msg4);

        NotificationMessage msg5 = new NotificationMessage();
        msg5.setUserId(2L);
        msg5.setType("CONTRACT");
        msg5.setTitle("合同签署完成");
        msg5.setContent("合同CON20260214002双方已签署完成，租赁关系正式生效。您可以下载合同查看详情。");
        msg5.setBizId(2L);
        msg5.setIsRead(1);
        msg5.setCreatedAt(now);
        messageMapper.insert(msg5);

        log.info("测试消息数据插入成功，共插入5条消息");
    }

    @Override
    public void deleteById(Long messageId) {
        messageMapper.deleteById(messageId);
        log.info("通知消息已删除: messageId={}", messageId);
    }

    @Override
    @PostConstruct
    public void fixTemplateData() {
        log.info("开始修复通知模板数据（含字符集修复）...");

        // 第一步：修复数据库表字符集为 utf8mb4
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE notification_template CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate.execute(
                    "ALTER TABLE notification_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            log.info("数据库表字符集已修复为 utf8mb4");
        } catch (Exception e) {
            log.warn("ALTER TABLE 字符集修复失败（可能已是 utf8mb4）: {}", e.getMessage());
        }

        // 第二步：用原生 SQL 直接覆写模板内容（避免 MyBatis 缓存等干扰）
        jdbcTemplate.update(
                "UPDATE notification_template SET title_template = ?, content_template = ? WHERE code = ?",
                "支付成功，租赁确认",
                "您好！您已成功支付订单 {{orderNo}} 的租金 ¥{{amount}} 元，房源【{{houseTitle}}】已正式租赁给您。合同将在稍后生成，请留意消息通知。",
                "TENANT_PAY_SUCCESS");

        jdbcTemplate.update(
                "UPDATE notification_template SET title_template = ?, content_template = ? WHERE code = ?",
                "租金已到账，房源已出租",
                "您好！您的房源【{{houseTitle}}】已被成功租赁，订单号 {{orderNo}} 的租金 ¥{{amount}} 元已到账，请在\"我的钱包\"查看余额。",
                "LANDLORD_RENT_RECEIVED");

        jdbcTemplate.update(
                "UPDATE notification_template SET title_template = ?, content_template = ? WHERE code = ?",
                "租赁合同已生成，请查看并签署",
                "您好！房源【{{houseTitle}}】的租赁合同（合同编号：{{contractNo}}）已生成。请登录系统查看合同详情并完成签署，双方签署后租赁关系正式生效。",
                "CONTRACT_CREATED_NOTICE");

        log.info("模板内容已覆写完成");

        // 补充退款相关通知模板（若不存在则插入，若存在则更新）
        upsertTemplate(
                "LANDLORD_REFUND_NOTICE",
                "租客退租通知",
                "您好！您的房源【{{houseTitle}}】的租客已申请退租（订单号：{{orderNo}}），租金 ¥{{amount}} 元已扣还给租客。房源已恢复为可租赁状态，欢迎发布新的招租信息。",
                "CONTRACT");

        upsertTemplate(
                "TENANT_REFUND_SUCCESS",
                "退款已到账",
                "您好！您的退租申请（订单号：{{orderNo}}）已处理成功，房源【{{houseTitle}}】的退款 ¥{{amount}} 元已返还至您的钱包，请登录查看最新余额。",
                "PAYMENT");

        log.info("退款通知模板补充完成");

        log.info("通知模板数据修复完成！");
    }

    /**
     * 按 code 更新模板的标题和内容
     */
    private void updateTemplate(String code, String title, String content) {
        LambdaQueryWrapper<NotificationTemplate> query = new LambdaQueryWrapper<>();
        query.eq(NotificationTemplate::getCode, code);
        NotificationTemplate template = templateMapper.selectOne(query);
        if (template != null) {
            template.setTitleTemplate(title);
            template.setContentTemplate(content);
            templateMapper.updateById(template);
            log.info("模板已修复: code={}", code);
        } else {
            log.warn("模板不存在: code={}", code);
        }
    }

    /**
     * upsert 模板：不存则插入，已存则更新
     *
     * @param code    模板 code
     * @param title   模板标题
     * @param content 模板内容
     * @param type    逻辑类型（CONTRACT / PAYMENT 等）
     */
    private void upsertTemplate(String code, String title, String content, String type) {
        try {
            LambdaQueryWrapper<NotificationTemplate> query = new LambdaQueryWrapper<>();
            query.eq(NotificationTemplate::getCode, code);
            NotificationTemplate existing = templateMapper.selectOne(query);
            if (existing != null) {
                // 已存在，更新内容
                existing.setTitleTemplate(title);
                existing.setContentTemplate(content);
                templateMapper.updateById(existing);
                log.info("模板已更新: code={}", code);
            } else {
                // 不存在，新君插入
                NotificationTemplate tpl = new NotificationTemplate();
                tpl.setCode(code);
                tpl.setName(title); // 用标题作为模板名称
                tpl.setType(type);
                tpl.setTitleTemplate(title);
                tpl.setContentTemplate(content);
                tpl.setEnabled(1);
                templateMapper.insert(tpl);
                log.info("模板已新增: code={}", code);
            }
        } catch (Exception e) {
            log.error("模板 upsert 失败: code={}", code, e);
        }
    }
}
