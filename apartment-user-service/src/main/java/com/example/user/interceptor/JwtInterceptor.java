package com.example.user.interceptor;

import com.example.user.dto.UserInfoDTO;
import com.example.user.utils.JWTUtils;
import com.example.user.utils.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JWTUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行健康检查接口
        if (request.getRequestURI().contains("/health") || request.getRequestURI().contains("/login")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\"}");
            return false;
        }

        try {
            if (!jwtUtils.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
                return false;
            }

            // 解析用户信息并设置到上下文
            Long userId = jwtUtils.getUserIdFromToken(token);
            io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
            
            UserInfoDTO userInfo = new UserInfoDTO();
            userInfo.setUserId(userId);
            userInfo.setUsername(claims.get("username", String.class));
            userInfo.setPhone(claims.get("phone", String.class));
            userInfo.setRole(claims.get("role", String.class));
            
            UserContextHolder.setUser(userInfo);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Token解析失败\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContextHolder.clear();
    }
}






