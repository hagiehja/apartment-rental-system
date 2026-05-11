package com.example.user.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO
 * 用于接收前端传入的账号（用户名或手机号）和密码
 */
@Data
public class UserLoginDTO {
    /**
     * 账号：可以是用户名，也可以是手机号
     * 前端根据实际输入传递，后端兼容两种方式查询
     */
    @NotBlank(message = "账号不能为空")
    private String account;
    /**
     * 密码（明文）
     * 注意：前端不应加密，由服务端进行 BCrypt 比对
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}