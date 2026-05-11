package com.example.house.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 房源查询条件DTO
 */
@Data
public class HouseQueryDTO implements Serializable {

    private String city; // 城市

    private String district; // 区县

    private String rentType; // 出租类型

    private BigDecimal minPrice; // 最低价格

    private BigDecimal maxPrice; // 最高价格

    private Integer minRoomCount; // 最少房间数

    private Integer maxRoomCount; // 最多房间数

    private String status; // 状态

    private Integer pageNum = 1; // 页码

    private Integer pageSize = 10; // 每页数量

    private String sortBy = "create_time"; // 排序字段

    private String sortOrder = "desc"; // 排序方向
}
