package com.example.house.service;

import com.example.house.dto.*;
import com.example.house.model.PageResult;

/**
 * 房源服务接口
 */
public interface HouseService {

    /**
     * 发布房源
     */
    Long publishHouse(HousePublishDTO publishDTO, Long landlordId);

    /**
     * 分页查询房源列表
     */
    PageResult<HouseListDTO> listHouses(HouseQueryDTO queryDTO);

    /**
     * 获取房源详情
     */
    HouseDetailDTO getHouseDetail(Long houseId);

    /**
     * 更新房源信息
     */
    void updateHouse(Long houseId, HousePublishDTO publishDTO, Long landlordId);

    /**
     * 删除房源
     */
    void deleteHouse(Long houseId, Long landlordId);

    /**
     * 下架房源
     */
    void offlineHouse(Long houseId, Long landlordId);

    /**
     * 上架房源
     */
    void onlineHouse(Long houseId, Long landlordId);

    /**
     * 更新房源状态（供其他服务调用）
     * @param houseId 房源ID
     * @param status 状态：AVAILABLE-可租赁, RENTED-已租赁, OFFLINE-已下架
     */
    void updateHouseStatus(Long houseId, String status);
}
