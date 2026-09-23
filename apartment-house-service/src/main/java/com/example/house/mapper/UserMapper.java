package com.example.house.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户表只读 Mapper (house-service 与 user-service 共享 apartment_db)
 * 仅用于联表查询房东信息,不进行写操作
 */
@Mapper
public interface UserMapper {

    /**
     * 批量查询用户信息 (用于房源列表展示房东名)
     */
    @Select({
        "<script>",
        "SELECT user_id AS userId, username, phone, role",
        "FROM user WHERE user_id IN ",
        "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<Map<String, Object>> selectBatchUserInfo(List<Long> userIds);

    /**
     * 查询单个用户 (用于房源详情)
     */
    @Select("SELECT user_id AS userId, username, phone, role FROM user WHERE user_id = #{userId}")
    Map<String, Object> selectUserInfo(Long userId);

    /**
     * 统计某房东的房源数量
     */
    @Select("SELECT COUNT(*) FROM house WHERE landlord_id = #{landlordId}")
    Integer countHousesByLandlord(Long landlordId);
}
