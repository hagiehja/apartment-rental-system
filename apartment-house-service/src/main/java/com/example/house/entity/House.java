package com.example.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源实体类
 */
@Data
@TableName("house")
public class House implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long houseId; // 房源ID

    private Long landlordId; // 房东用户ID

    private String title; // 房源标题

    private String description; // 房源描述

    private String province; // 省份

    private String city; // 城市

    private String district; // 区县

    private String address; // 详细地址

    private BigDecimal area; // 房屋面积

    private Integer roomCount; // 房间数

    private Integer hallCount; // 厅数

    private Integer bathroomCount; // 卫生间数

    private Integer floor; // 楼层

    private Integer totalFloor; // 总楼层

    private String orientation; // 朝向

    private String decoration; // 装修情况

    private String rentType; // 出租类型（WHOLE-整租/SHARED-合租）

    private BigDecimal price; // 租金（元/月）

    private String paymentMethod; // 付款方式

    private String facilities; // 配套设施（JSON字符串）

    private String status; // 状态（AVAILABLE/RENTED/OFFLINE）

    private Integer viewCount; // 浏览次数

    /**
     * 乐观锁版本号，防止并发超卖
     * MyBatis-Plus 自动在 UPDATE 时校验并递增
     */
    @Version
    private Integer version;

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间
}
