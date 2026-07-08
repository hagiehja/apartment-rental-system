package com.example.house.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class BehaviorTrackDTO implements Serializable {

    private Long userId;

    @NotNull(message = "houseId is required")
    private Long houseId;

    @NotNull(message = "behaviorType is required")
    private String behaviorType;

    private String source;
}
