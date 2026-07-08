package com.example.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

/**
 * 房源服务 Feign 客户端
 * 用于订单服务调用房源服务
 */
@FeignClient(name = "apartment-house-service", url = "${feign.url.house-service:http://apartment-house-service:8083}")
public interface HouseFeignClient {

    /**
     * 获取房源详情
     * 
     * @param houseId 房源ID
     * @return 房源信息
     */
    @GetMapping("/house/{id}")
    Map<String, Object> getHouseDetail(@PathVariable("id") Long houseId);

    /**
     * 更新房源状态
     * 
     * @param houseId 房源ID
     * @param status 状态：AVAILABLE-可租赁, RENTED-已租赁, OFFLINE-已下架
     * @return 操作结果
     */
    @PutMapping("/house/{id}/status")
    Map<String, Object> updateHouseStatus(@PathVariable("id") Long houseId, 
                                          @RequestParam String status);
}
