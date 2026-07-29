package com.example.user.vo;

import lombok.Data;

/**
 * 用户角色统计 VO
 */
@Data
public class RoleStatsVO {
    private Long totalUsers;
    private Long landlords;
    private Long tenants;
    private Long admins;
}
