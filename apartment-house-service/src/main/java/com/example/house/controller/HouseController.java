package com.example.house.controller;

import com.example.house.dto.*;
import com.example.house.model.PageResult;
import com.example.common.api.Result;
import com.example.house.recommendation.fm.FmModelLoader;
import com.example.house.service.HouseService;
import com.example.house.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 房源控制器
 * 
 * 注意：实际生产环境中需要从JWT Token中获取当前用户ID
 * 这里为了简化，暂时通过请求头传递landlordId
 */
@RestController
@RequestMapping("/house")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;
    private final RecommendationService recommendationService;
    private final FmModelLoader fmModelLoader;

    /**
     * 发布房源
     */
    @PostMapping
    public Result<Long> publishHouse(@Valid @RequestBody HousePublishDTO publishDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // TODO: 从JWT Token中获取userId
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        Long houseId = houseService.publishHouse(publishDTO, userId);
        return Result.success(houseId);
    }

    /**
     * 房源列表（分页查询）
     */
    @GetMapping("/list")
    public Result<PageResult<HouseListDTO>> listHouses(HouseQueryDTO queryDTO) {
        PageResult<HouseListDTO> pageResult = houseService.listHouses(queryDTO);
        return Result.success(pageResult);
    }

    /**
     * 房源详情
     */
    @GetMapping("/{id}")
    public Result<HouseDetailDTO> getHouseDetail(@PathVariable("id") Long houseId) {
        HouseDetailDTO detail = houseService.getHouseDetail(houseId);
        if (detail == null) {
            return Result.error("房源不存在");
        }
        return Result.success(detail);
    }

    /**
     * 更新房源
     */
    @PutMapping("/{id}")
    public Result<Void> updateHouse(@PathVariable("id") Long houseId,
            @Valid @RequestBody HousePublishDTO publishDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        houseService.updateHouse(houseId, publishDTO, userId);
        return Result.success(null);
    }

    /**
     * 删除房源
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteHouse(@PathVariable("id") Long houseId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        houseService.deleteHouse(houseId, userId);
        return Result.success(null);
    }

    /**
     * 下架房源
     */
    @PutMapping("/{id}/offline")
    public Result<Void> offlineHouse(@PathVariable("id") Long houseId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        houseService.offlineHouse(houseId, userId);
        return Result.success(null);
    }

    /**
     * 上架房源
     */
    @PutMapping("/{id}/online")
    public Result<Void> onlineHouse(@PathVariable("id") Long houseId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        houseService.onlineHouse(houseId, userId);
        return Result.success(null);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("House Service is running!");
    }

    /**
     * 更新房源状态（供其他服务内部调用）
     * @param houseId 房源ID
     * @param status 状态：AVAILABLE-可租赁, RENTED-已租赁, OFFLINE-已下架
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long houseId,
                                      @RequestParam String status) {
        houseService.updateHouseStatus(houseId, status);
        return Result.success(null);
    }

    // ============================================================
    // 推荐系统接口(FM + TensorFlow 训练)
    // ============================================================

    /**
     * 个性化推荐(基于用户偏好 + TensorFlow FM 打分)
     * Header X-User-Id 必传
     */
    @GetMapping("/recommend")
    public Result<PageResult<HouseRecommendDTO>> recommend(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (userId == null) {
            return Result.error(401, "未登录,无法获取个性化推荐");
        }
        PageResult<HouseRecommendDTO> page = recommendationService.recommend(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 行为埋点上报(VIEW/CLICK/FAVORITE/ORDER/PAY)
     * 用于训练数据收集,后续模型迭代会基于这些行为
     */
    @PostMapping("/behavior/track")
    public Result<Void> trackBehavior(@Valid @RequestBody BehaviorTrackDTO trackDTO,
                                       @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        recommendationService.trackBehavior(trackDTO, headerUserId);
        return Result.success(null);
    }

    /**
     * 保存/更新用户偏好(用于推荐召回)
     */
    @PostMapping("/preference")
    public Result<Void> savePreference(@Valid @RequestBody UserPreferenceDTO preferenceDTO,
                                        @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        recommendationService.savePreference(preferenceDTO, headerUserId);
        return Result.success(null);
    }

    /**
     * 查询当前用户偏好
     */
    @GetMapping("/preference")
    public Result<UserPreferenceDTO> getPreference(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        if (headerUserId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(recommendationService.getPreference(headerUserId));
    }

    /**
     * FM 模型信息(暴露 TensorFlow 训练元数据,让前端"看得见"算法)
     */
    @GetMapping("/recommend/model-info")
    public Result<Map<String, Object>> modelInfo() {
        return Result.success(fmModelLoader.getModelInfo());
    }
}
