package com.example.house.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_preference")
public class UserPreference implements Serializable {

    @TableId
    private Long userId;

    private String city;

    private String district;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer roomCount;

    private BigDecimal minArea;

    private BigDecimal maxArea;

    private String rentType;

    private String commuteAddress;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
