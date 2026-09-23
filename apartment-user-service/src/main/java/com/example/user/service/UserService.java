package com.example.user.service;

import com.example.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.enums.UserRole;
import com.example.user.dto.UserInfoDTO;
import com.example.user.dto.UserLoginDTO;
import com.example.user.dto.UserRegisterDTO;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.utils.JWTUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;

    /**
     * 用户注册
     * - 用户名/手机号唯一性校验
     * - 密码 BCrypt 加密
     * - 角色只能是 TENANT 或 LANDLORD
     */
    public UserInfoDTO register(UserRegisterDTO dto) {
        // 1. 用户名唯一性校验
        Long usernameCount = userMapper.selectCount(
                new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new IllegalArgumentException("用户名已被使用,请更换");
        }

        // 2. 手机号唯一性校验
        Long phoneCount = userMapper.selectCount(
                new QueryWrapper<User>().eq("phone", dto.getPhone()));
        if (phoneCount != null && phoneCount > 0) {
            throw new IllegalArgumentException("手机号已注册");
        }

        // 3. 构造实体并加密密码
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 4. 构造返回 DTO (含 token, 注册后可直接登录)
        UserInfoDTO info = new UserInfoDTO();
        info.setUserId(user.getUserId());
        info.setUsername(user.getUsername());
        info.setPhone(user.getPhone());
        info.setRole(user.getRole());
        info.setToken(jwtUtils.generateToken(info));
        return info;
    }

    /**
     * 批量查询用户简要信息(供其他服务调用, 例如房源服务展示房东名)
     */
    public java.util.Map<Long, com.example.user.vo.UserBriefVO> batchUserInfo(java.util.List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return java.util.Collections.emptyMap();
        java.util.List<User> users = userMapper.selectList(
                new QueryWrapper<User>().in("user_id", userIds));
        java.util.Map<Long, com.example.user.vo.UserBriefVO> result = new java.util.HashMap<>();
        for (User u : users) {
            com.example.user.vo.UserBriefVO vo = new com.example.user.vo.UserBriefVO();
            vo.setUserId(u.getUserId());
            vo.setUsername(u.getUsername());
            vo.setPhone(u.getPhone());
            vo.setRole(u.getRole());
            result.put(u.getUserId(), vo);
        }
        return result;
    }

    /**
     * 角色统计 (返回各角色用户数)
     */
    public com.example.user.vo.RoleStatsVO getRoleStats() {
        com.example.user.vo.RoleStatsVO stats = new com.example.user.vo.RoleStatsVO();
        stats.setTotalUsers(userMapper.selectCount(null));
        stats.setLandlords(userMapper.selectCount(
                new QueryWrapper<User>().eq("role", UserRole.LANDLORD.name())));
        stats.setTenants(userMapper.selectCount(
                new QueryWrapper<User>().eq("role", UserRole.TENANT.name())));
        stats.setAdmins(userMapper.selectCount(
                new QueryWrapper<User>().eq("role", UserRole.ADMIN.name())));
        return stats;
    }

    /**
     * 判断指定用户是否是管理员
     */
    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        User u = userMapper.selectById(userId);
        return u != null && UserRole.ADMIN.name().equals(u.getRole());
    }

    /**
     * 管理员: 分页查询用户列表 (含 role / keyword 过滤)
     * 返回字段: total / pageNum / pageSize / records
     */
    public com.example.common.api.PageResult<com.example.user.vo.UserAdminVO> adminListUsers(String role, String keyword,
                                                       Integer pageNum, Integer pageSize) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            qw.eq("role", role);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("username", keyword).or().like("phone", keyword));
        }
        qw.orderByDesc("user_id");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = userMapper.selectPage(page, qw);

        java.util.List<com.example.user.vo.UserAdminVO> records = new java.util.ArrayList<>();
        for (User u : result.getRecords()) {
            com.example.user.vo.UserAdminVO vo = new com.example.user.vo.UserAdminVO();
            vo.setUserId(u.getUserId());
            vo.setUsername(u.getUsername());
            vo.setPhone(u.getPhone());
            vo.setRole(u.getRole());
            vo.setCreateTime(u.getCreateTime());
            records.add(vo);
        }
        return new com.example.common.api.PageResult<>(result.getTotal(), pageNum, pageSize, records);
    }

    /**
     * 管理员: 修改用户角色
     */
    public void adminUpdateRole(Long targetUserId, String newRole) {
        User u = userMapper.selectById(targetUserId);
        if (u == null) {
            throw new BusinessException("目标用户不存在");
        }
        u.setRole(newRole);
        u.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(u);
    }

    /**
     * 用户登录方法
     * 支持使用用户名或手机号登录
     * 
     * @param loginDTO 登录请求数据（账号+密码）
     * @return 登录成功返回用户信息和JWT Token，失败返回null
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserInfoDTO login(UserLoginDTO loginDTO) {
        // [VULN-05 修复] 移除所有明文密码日志,避免泄露到日志系统
        // 只记录账号(便于审计),不记录密码
        log.info("登录尝试 account={}", loginDTO.getAccount());

        // 根据账号查询用户（支持用户名或手机号）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", loginDTO.getAccount())
                .or()
                .eq("phone", loginDTO.getAccount());

        User user = userMapper.selectOne(queryWrapper);

        // 用户不存在
        if (user == null) {
            log.warn("登录失败 用户不存在 account={}", loginDTO.getAccount());
            return null;
        }

        // 验证密码 (BCrypt 比对,不记录明文)
        boolean passwordMatches = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());

        if (!passwordMatches) {
            log.warn("登录失败 密码错误 account={}", loginDTO.getAccount());
            return null;
        }

        // 构造用户信息DTO
        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole());

        // 生成JWT Token
        String token = jwtUtils.generateToken(userInfo);
        userInfo.setToken(token);

        return userInfo;
    }

    /**
     * Spring Security 使用此方法加载用户详情
     * 用于JWT认证时验证用户身份
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username)
                .or()
                .eq("phone", username);

        // 兼容JWTUtils将userId放入sub的情况:如果传入的是纯数字,也尝试按userId查
        if (username != null && username.matches("\\d+")) {
            queryWrapper.or().eq("user_id", Long.parseLong(username));
        }

        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 构造Spring Security的UserDetails对象
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}

