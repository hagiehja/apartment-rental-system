package com.example.user.dto;
import lombok.Data;
import java.io.Serializable;

@Data
public class UserInfoDTO implements Serializable {
    private Long userId;
    private String username;
    private String phone;
    private String role; // TENANT-租客, LANDLORD-房东, ADMIN-管理员
    private String token; // JWT token

}