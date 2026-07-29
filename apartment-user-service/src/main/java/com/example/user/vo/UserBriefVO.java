package com.example.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户简要信息 VO(用于跨服务批量查询返回)
 */
@Data
public class UserBriefVO implements Serializable {
    private Long userId;
    private String username;
    private String phone;
    private String role;
}
