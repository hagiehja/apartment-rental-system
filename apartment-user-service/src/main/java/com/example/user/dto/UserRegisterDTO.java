package com.example.user.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册请求 DTO
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[\\p{IsHan}a-zA-Z0-9_]{2,20}$",
             message = "用户名只能包含中英文、数字和下划线,长度2-20")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度6-20位")
    private String password;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(TENANT|LANDLORD)$",
             message = "角色只能是 TENANT(租客) 或 LANDLORD(房东)")
    private String role;
}
