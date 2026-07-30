package com.example.user.controller;

import com.example.user.dto.UserInfoDTO;
import com.example.user.dto.UserLoginDTO;
import com.example.user.dto.UserRegisterDTO;
import com.example.common.api.Result;
import com.example.common.enums.UserRole;
import com.example.user.service.UserService;
import com.example.user.vo.RoleStatsVO;
import com.example.user.vo.UserAdminVO;
import com.example.user.vo.UserBriefVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
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
        UserInfoDTO userInfo = userService.login(loginDTO);
        if (userInfo == null) {
            return Result.error("用户名或密码错误");
        }
        return Result.success(userInfo);
    }

    /**
     * 用户注册
     * 支持 TENANT(租客) / LANDLORD(房东) 两种角色
     */
    @PostMapping("/register")
    public Result<UserInfoDTO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        try {
            UserInfoDTO userInfo = userService.register(registerDTO);
            return Result.success(userInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册异常 username={}", registerDTO.getUsername(), e);
            return Result.error("注册失败,请稍后再试");
        }
    }

    /**
     * 批量查询用户信息 (给其他服务调用, 例如房源服务要显示房东名)
     * GET /user/batch?ids=1,2,3
     * [安全] 此接口在 nginx 公开白名单内(无需登录),手机号必须脱敏,防止批量爬取 PII。
     *        需要手机号的场景走登录后的鉴权接口。
     */
    @GetMapping("/batch")
    public Result<Map<Long, UserBriefVO>> batchUserInfo(@RequestParam("ids") List<Long> ids) {
        Map<Long, UserBriefVO> result = userService.batchUserInfo(ids);
        result.values().forEach(vo -> vo.setPhone(null));
        return Result.success(result);
    }

    /**
     * 统计接口: 角色分布
     */
    @GetMapping("/stats")
    public Result<RoleStatsVO> stats() {
        return Result.success(userService.getRoleStats());
    }

    /**
     * 管理员接口: 分页查询全部用户
     * 需要 X-User-Id 请求头,且该用户角色必须是 ADMIN
     * 支持 role / keyword 过滤
     */
    @GetMapping("/admin/list")
    public Result<com.example.common.api.PageResult<UserAdminVO>> adminListUsers(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        // 权限校验:必须是 ADMIN
        if (userId == null) {
            return Result.error("未登录");
        }
        if (!userService.isAdmin(userId)) {
            return Result.error("无权限访问管理后台");
        }
        com.example.common.api.PageResult<UserAdminVO> result =
                userService.adminListUsers(role, keyword, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 管理员接口: 修改用户角色
     */
    @PutMapping("/admin/{targetUserId}/role")
    public Result<Void> adminUpdateUserRole(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("targetUserId") Long targetUserId,
            @RequestBody java.util.Map<String, String> body) {
        if (userId == null) {
            return Result.error("未登录");
        }
        if (!userService.isAdmin(userId)) {
            return Result.error("无权限访问管理后台");
        }
        String newRole = body.get("role");
        if (UserRole.of(newRole) == null) {
            return Result.error("角色只能为 TENANT / LANDLORD / ADMIN");
        }
        userService.adminUpdateRole(targetUserId, newRole);
        return Result.success(null);
    }
}



