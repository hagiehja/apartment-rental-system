package com.example.house.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发布房源DTO
 */
@Data
public class HousePublishDTO implements Serializable {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    private String district;

    @NotBlank(message = "地址不能为空")
    private String address;

    private BigDecimal area;

    private Integer roomCount;

    private Integer hallCount;

    private Integer bathroomCount;

    private Integer floor;

    private Integer totalFloor;

    private String orientation;

    private String decoration;

    @NotBlank(message = "出租类型不能为空")
    private String rentType; // WHOLE-整租 / SHARED-合租

    @NotNull(message = "租金不能为空")
    private BigDecimal price;

    private String paymentMethod;

    private List<String> facilities; // 配套设施列表

    private List<String> imageUrls; // 图片URL列表

    private Integer coverImageIndex; // 封面图索引（默认0）
}
