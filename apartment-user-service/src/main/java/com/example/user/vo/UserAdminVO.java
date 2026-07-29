package com.example.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员视角的用户列表项 VO
 */
@Data
public class UserAdminVO implements Serializable {
    private Long userId;
    private String username;
    private String phone;
    private String role;
    private LocalDateTime createTime;
}
