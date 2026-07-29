package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long userId; // 主键 ID

    private String username; // 用户名（唯一）

    private String phone; // 手机号（唯一，可用于登录）

    private String password; // 密码（BCrypt 加密后存储）

    private String role; // 角色：TENANT, LANDLORD, ADMIN

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime; // 更新时间
}