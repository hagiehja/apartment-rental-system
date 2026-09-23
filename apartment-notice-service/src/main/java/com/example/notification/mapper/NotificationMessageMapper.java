package com.example.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.notification.entity.NotificationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知消息Mapper
 */
@Mapper
public interface NotificationMessageMapper extends BaseMapper<NotificationMessage> {
}
