package com.example.user.controller;

import com.example.user.dto.UserInfoDTO;
import com.example.user.dto.UserLoginDTO;
import com.example.user.model.Result;
import com.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("User Service is running successfully!");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserInfoDTO> login(@RequestBody UserLoginDTO loginDTO) {
        System.out.println("========== Controller 收到登录请求 ==========");
        System.out.println("请求参数: " + loginDTO);

        UserInfoDTO userInfo = userService.login(loginDTO);

        if (userInfo == null) {
            System.out.println("❌ 登录失败：返回错误响应");
            Result<UserInfoDTO> errorResult = Result.error("用户名或密码错误");
            System.out.println("错误响应: " + errorResult);
            return errorResult;
        }

        System.out.println("✅ 登录成功！用户信息:");
        System.out.println("  - userId: " + userInfo.getUserId());
        System.out.println("  - username: " + userInfo.getUsername());
        System.out.println("  - phone: " + userInfo.getPhone());
        System.out.println("  - role: " + userInfo.getRole());
        System.out.println("  - token: " + (userInfo.getToken() != null
                ? userInfo.getToken().substring(0, Math.min(30, userInfo.getToken().length())) + "..."
                : "null"));

        Result<UserInfoDTO> successResult = Result.success(userInfo);
        System.out.println("成功响应: " + successResult);
        return successResult;
    }
}



