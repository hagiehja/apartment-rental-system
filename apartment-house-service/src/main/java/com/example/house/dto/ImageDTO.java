package com.example.house.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 图片DTO
 */
@Data
public class ImageDTO implements Serializable {

    private Long imageId;

    private String imageUrl;

    private Integer isCover;

    private Integer sortOrder;
}
