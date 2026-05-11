package com.example.house.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房源图片实体类
 */
@Data
@TableName("house_image")
public class HouseImage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long imageId; // 图片ID

    private Long houseId; // 房源ID

    private String imageUrl; // 图片URL

    private Integer isCover; // 是否封面图（0-否/1-是）

    private Integer sortOrder; // 排序

    private LocalDateTime createTime; // 创建时间
}
