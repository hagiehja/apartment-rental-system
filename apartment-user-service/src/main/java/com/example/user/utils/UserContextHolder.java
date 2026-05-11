package com.example.user.utils;

import com.example.user.dto.UserInfoDTO;

public class UserContextHolder {
    
    private static final ThreadLocal<UserInfoDTO> userContext = new ThreadLocal<>();

    public static void setUser(UserInfoDTO user) {
        userContext.set(user);
    }

    public static UserInfoDTO getUser() {
        return userContext.get();
    }

    public static void clear() {
        userContext.remove();
    }
}
