package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.user.dto.UserInfoDTO;
import com.example.user.dto.UserLoginDTO;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.utils.JWTUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;

    /**
     * 用户登录方法
     * 支持使用用户名或手机号登录
     * 
     * @param loginDTO 登录请求数据（账号+密码）
     * @return 登录成功返回用户信息和JWT Token，失败返回null
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserInfoDTO login(UserLoginDTO loginDTO) {
        System.out.println("========== 登录调试信息 ==========");
        System.out.println("登录账号: " + loginDTO.getAccount());
        System.out.println("登录密码: " + loginDTO.getPassword());

        // 根据账号查询用户（支持用户名或手机号）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", loginDTO.getAccount())
                .or()
                .eq("phone", loginDTO.getAccount());

        User user = userMapper.selectOne(queryWrapper);

        // 用户不存在
        if (user == null) {
            System.out.println("❌ 用户不存在: " + loginDTO.getAccount());
            return null;
        }

        System.out.println("✅ 找到用户: " + user.getUsername());
        System.out
                .println("数据库密码(前30位): " + user.getPassword().substring(0, Math.min(30, user.getPassword().length())));
        System.out.println("密码是否以$2a开头: " + user.getPassword().startsWith("$2a"));

        // 验证密码
        boolean passwordMatches = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());
        System.out.println("密码匹配结果: " + (passwordMatches ? "✅ 成功" : "❌ 失败"));

        if (!passwordMatches) {
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

