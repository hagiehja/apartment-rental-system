package com.example.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payment.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户账户Mapper
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 根据用户ID查询账户（加行锁）
     */
    @Select("SELECT * FROM user_account WHERE user_id = #{userId} FOR UPDATE")
    UserAccount selectByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 根据用户ID查询账户
     */
    @Select("SELECT * FROM user_account WHERE user_id = #{userId}")
    UserAccount selectByUserId(@Param("userId") Long userId);
}
