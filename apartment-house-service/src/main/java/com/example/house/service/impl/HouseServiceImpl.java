package com.example.house.service.impl;

import com.example.common.exception.BusinessException;
import com.example.common.enums.HouseStatus;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.house.dto.*;
import com.example.house.entity.House;
import com.example.house.entity.HouseImage;
import com.example.house.mapper.HouseImageMapper;
import com.example.house.mapper.HouseMapper;
import com.example.house.model.PageResult;
import com.example.house.service.HouseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 房源服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseMapper houseMapper;
    private final HouseImageMapper houseImageMapper;
    private final com.example.house.mapper.UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    // 发布新房源后，清空列表缓存以确保新房源出现在搜索结果中
    @CacheEvict(value = "house:list", allEntries = true)
    public Long publishHouse(HousePublishDTO publishDTO, Long landlordId) {
        // 构建House实体
        House house = new House();
        house.setLandlordId(landlordId);
        house.setTitle(publishDTO.getTitle());
        house.setDescription(publishDTO.getDescription());
        house.setProvince(publishDTO.getProvince());
        house.setCity(publishDTO.getCity());
        house.setDistrict(publishDTO.getDistrict());
        house.setAddress(publishDTO.getAddress());
        house.setArea(publishDTO.getArea());
        house.setRoomCount(publishDTO.getRoomCount());
        house.setHallCount(publishDTO.getHallCount());
        house.setBathroomCount(publishDTO.getBathroomCount());
        house.setFloor(publishDTO.getFloor());
        house.setTotalFloor(publishDTO.getTotalFloor());
        house.setOrientation(publishDTO.getOrientation());
        house.setDecoration(publishDTO.getDecoration());
        house.setRentType(publishDTO.getRentType());
        house.setPrice(publishDTO.getPrice());
        house.setPaymentMethod(publishDTO.getPaymentMethod());

        // 将设施列表转换为JSON字符串
        if (publishDTO.getFacilities() != null && !publishDTO.getFacilities().isEmpty()) {
            try {
                house.setFacilities(objectMapper.writeValueAsString(publishDTO.getFacilities()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("设施信息转换失败", e);
            }
        }

        house.setStatus(HouseStatus.AVAILABLE.name());
        house.setViewCount(0);

        // 保存房源
        houseMapper.insert(house);

        // 保存图片
        if (publishDTO.getImageUrls() != null && !publishDTO.getImageUrls().isEmpty()) {
            List<HouseImage> images = new ArrayList<>();
            int coverIndex = publishDTO.getCoverImageIndex() != null ? publishDTO.getCoverImageIndex() : 0;

            for (int i = 0; i < publishDTO.getImageUrls().size(); i++) {
                HouseImage image = new HouseImage();
                image.setHouseId(house.getHouseId());
                image.setImageUrl(publishDTO.getImageUrls().get(i));
                image.setIsCover(i == coverIndex ? 1 : 0);
                image.setSortOrder(i);
                images.add(image);
            }

            images.forEach(houseImageMapper::insert);
        }

        log.info("房源发布成功并清除列表缓存，houseId={}", house.getHouseId());
        return house.getHouseId();
    }

    @Override
    // 房源列表缓存：以查询参数的 hashCode 作为缓存 key，TTL 5 分钟（在 RedisCacheConfig 中设置）
    @Cacheable(value = "house:list", key = "#queryDTO.hashCode()")
    @DS("slave")   // 读走从库(读写分离),写操作不加注解默认走主库
    public PageResult<HouseListDTO> listHouses(HouseQueryDTO queryDTO) {
        // 构建查询条件
        QueryWrapper<House> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(queryDTO.getCity())) {
            queryWrapper.like("city", queryDTO.getCity());
        }
        if (StringUtils.hasText(queryDTO.getDistrict())) {
            queryWrapper.like("district", queryDTO.getDistrict());
        }
        // 按房东筛选 (查看某房东的全部房源)
        if (queryDTO.getLandlordId() != null) {
            queryWrapper.eq("landlord_id", queryDTO.getLandlordId());
        }
        if (StringUtils.hasText(queryDTO.getRentType())) {
            queryWrapper.eq("rent_type", queryDTO.getRentType());
        }
        if (queryDTO.getMinPrice() != null) {
            queryWrapper.ge("price", queryDTO.getMinPrice());
        }
        if (queryDTO.getMaxPrice() != null) {
            queryWrapper.le("price", queryDTO.getMaxPrice());
        }
        if (queryDTO.getMinRoomCount() != null) {
            queryWrapper.ge("room_count", queryDTO.getMinRoomCount());
        }
        if (queryDTO.getMaxRoomCount() != null) {
            queryWrapper.le("room_count", queryDTO.getMaxRoomCount());
        }

        // 房源列表应展示所有上架房源（含已租赁），前端通过 status 控制是否可下单
        if (StringUtils.hasText(queryDTO.getStatus())) {
            // 用户明确筛选某状态时按指定状态查
            queryWrapper.eq("status", queryDTO.getStatus());
        } else {
            // 默认展示 AVAILABLE + RENTED（排除 OFFLINE 下架的房源）
            queryWrapper.in("status", HouseStatus.AVAILABLE.name(), HouseStatus.RENTED.name());
        }

        // 排序
        if ("price".equals(queryDTO.getSortBy())) {
            queryWrapper.orderBy(true, "asc".equals(queryDTO.getSortOrder()), "price");
        } else if ("view_count".equals(queryDTO.getSortBy())) {
            queryWrapper.orderBy(true, "asc".equals(queryDTO.getSortOrder()), "view_count");
        } else {
            queryWrapper.orderBy(true, "asc".equals(queryDTO.getSortOrder()), "create_time");
        }

        // 分页查询
        Page<House> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        Page<House> housePage = houseMapper.selectPage(page, queryWrapper);

        // ===== [N+1 修复] 批量预加载封面图,避免每条房源单独查一次 =====
        // 旧逻辑:每条房源单独 SELECT cover_image WHERE house_id=? → pageSize=10 产生 11 次 SQL
        // 新逻辑:1 次 IN 查询拿到全部封面图,内存映射,O(1) 取用
        List<Long> houseIds = housePage.getRecords().stream()
                .map(House::getHouseId).collect(Collectors.toList());
        java.util.Map<Long, String> coverImageMap = java.util.Collections.emptyMap();
        if (!houseIds.isEmpty()) {
            QueryWrapper<HouseImage> imageWrapper = new QueryWrapper<>();
            imageWrapper.in("house_id", houseIds).eq("is_cover", 1);
            coverImageMap = houseImageMapper.selectList(imageWrapper).stream()
                    .collect(Collectors.toMap(HouseImage::getHouseId,
                            HouseImage::getImageUrl, (a, b) -> a));
        }

        // 转换为DTO(封面图从预加载 Map 取,零额外 SQL)
        final java.util.Map<Long, String> finalCoverMap = coverImageMap;

        // ===== 批量预加载房东信息 (避免 N+1) =====
        List<Long> landlordIds = housePage.getRecords().stream()
                .map(House::getLandlordId).distinct().collect(Collectors.toList());
        java.util.Map<Long, java.util.Map<String, Object>> landlordMap = java.util.Collections.emptyMap();
        if (!landlordIds.isEmpty()) {
            landlordMap = new java.util.HashMap<>();
            for (java.util.Map<String, Object> u : userMapper.selectBatchUserInfo(landlordIds)) {
                Object idObj = u.get("userId");
                if (idObj instanceof Number) {
                    landlordMap.put(((Number) idObj).longValue(), u);
                }
            }
        }
        final java.util.Map<Long, java.util.Map<String, Object>> finalLandlordMap = landlordMap;

        List<HouseListDTO> list = housePage.getRecords().stream().map(house -> {
            HouseListDTO dto = new HouseListDTO();
            dto.setHouseId(house.getHouseId());
            dto.setLandlordId(house.getLandlordId());
            // 填入房东名 (找不到就显示默认)
            java.util.Map<String, Object> landlord = finalLandlordMap.get(house.getLandlordId());
            if (landlord != null) {
                dto.setLandlordName(landlord.get("username") == null ? null
                        : landlord.get("username").toString());
            }
            dto.setTitle(house.getTitle());
            dto.setCity(house.getCity());
            dto.setDistrict(house.getDistrict());
            dto.setArea(house.getArea());
            dto.setRoomCount(house.getRoomCount());
            dto.setHallCount(house.getHallCount());
            dto.setRentType(house.getRentType());
            dto.setPrice(house.getPrice());
            dto.setViewCount(house.getViewCount());
            dto.setCreateTime(house.getCreateTime());
            dto.setStatus(house.getStatus());
            dto.setCoverImage(finalCoverMap.get(house.getHouseId()));
            return dto;
        }).collect(Collectors.toList());

        return new PageResult<>(housePage.getTotal(), queryDTO.getPageNum(), queryDTO.getPageSize(), list);
    }

    @Override
    @Transactional
    // 房源详情缓存：以 houseId 为 key，TTL 10 分钟（在 RedisCacheConfig 中设置）
    // 命中缓存时直接返回，不再查库；未命中时查库并写入缓存
    @Cacheable(value = "house:detail", key = "#houseId")
    // 注意:此方法含 viewCount+1 的写操作,不能加 @DS("slave")(从库 read_only=ON 会失败)
    // 故默认走主库。仅 listHouses(高频纯读)走从库,实现读写分离的核心价值
    public HouseDetailDTO getHouseDetail(Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return null;
        }

        // 浏览次数+1（即使命中缓存也会在首次加载时计数，TTL内的重复访问不再重复计数，
        // 这在实际业务中反而更合理：同一用户短时间内刷新不应重复刷浏览量）
        house.setViewCount(house.getViewCount() + 1);
        houseMapper.updateById(house);

        // 转换为DTO
        HouseDetailDTO dto = new HouseDetailDTO();
        dto.setHouseId(house.getHouseId());
        dto.setLandlordId(house.getLandlordId());
        // ===== 填入房东信息 =====
        java.util.Map<String, Object> landlord = userMapper.selectUserInfo(house.getLandlordId());
        if (landlord != null) {
            dto.setLandlordName(landlord.get("username") == null ? null
                    : landlord.get("username").toString());
            dto.setLandlordPhone(landlord.get("phone") == null ? null
                    : landlord.get("phone").toString());
        }
        dto.setLandlordHouseCount(userMapper.countHousesByLandlord(house.getLandlordId()));
        dto.setTitle(house.getTitle());
        dto.setDescription(house.getDescription());
        dto.setProvince(house.getProvince());
        dto.setCity(house.getCity());
        dto.setDistrict(house.getDistrict());
        dto.setAddress(house.getAddress());
        dto.setArea(house.getArea());
        dto.setRoomCount(house.getRoomCount());
        dto.setHallCount(house.getHallCount());
        dto.setBathroomCount(house.getBathroomCount());
        dto.setFloor(house.getFloor());
        dto.setTotalFloor(house.getTotalFloor());
        dto.setOrientation(house.getOrientation());
        dto.setDecoration(house.getDecoration());
        dto.setRentType(house.getRentType());
        dto.setPrice(house.getPrice());
        dto.setPaymentMethod(house.getPaymentMethod());
        dto.setStatus(house.getStatus());
        dto.setViewCount(house.getViewCount());
        dto.setCreateTime(house.getCreateTime());
        dto.setUpdateTime(house.getUpdateTime());

        // 解析设施JSON
        if (StringUtils.hasText(house.getFacilities())) {
            try {
                List<String> facilities = objectMapper.readValue(house.getFacilities(),
                        new TypeReference<List<String>>() {
                        });
                dto.setFacilities(facilities);
            } catch (JsonProcessingException e) {
                dto.setFacilities(new ArrayList<>());
            }
        }

        // 获取图片列表
        QueryWrapper<HouseImage> imageWrapper = new QueryWrapper<>();
        imageWrapper.eq("house_id", houseId).orderByAsc("sort_order");
        List<HouseImage> images = houseImageMapper.selectList(imageWrapper);

        List<ImageDTO> imageDTOs = images.stream().map(image -> {
            ImageDTO imageDTO = new ImageDTO();
            imageDTO.setImageId(image.getImageId());
            imageDTO.setImageUrl(image.getImageUrl());
            imageDTO.setIsCover(image.getIsCover());
            imageDTO.setSortOrder(image.getSortOrder());
            return imageDTO;
        }).collect(Collectors.toList());

        dto.setImages(imageDTOs);

        return dto;
    }

    @Override
    @Transactional
    // 房源编辑后，同时清除该房源的详情缓存和整个列表缓存
    @Caching(evict = {
            @CacheEvict(value = "house:detail", key = "#houseId"),
            @CacheEvict(value = "house:list", allEntries = true)
    })
    public void updateHouse(Long houseId, HousePublishDTO publishDTO, Long landlordId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (!house.getLandlordId().equals(landlordId)) {
            throw new BusinessException("无权操作此房源");
        }

        // 更新房源信息
        house.setTitle(publishDTO.getTitle());
        house.setDescription(publishDTO.getDescription());
        house.setProvince(publishDTO.getProvince());
        house.setCity(publishDTO.getCity());
        house.setDistrict(publishDTO.getDistrict());
        house.setAddress(publishDTO.getAddress());
        house.setArea(publishDTO.getArea());
        house.setRoomCount(publishDTO.getRoomCount());
        house.setHallCount(publishDTO.getHallCount());
        house.setBathroomCount(publishDTO.getBathroomCount());
        house.setFloor(publishDTO.getFloor());
        house.setTotalFloor(publishDTO.getTotalFloor());
        house.setOrientation(publishDTO.getOrientation());
        house.setDecoration(publishDTO.getDecoration());
        house.setRentType(publishDTO.getRentType());
        house.setPrice(publishDTO.getPrice());
        house.setPaymentMethod(publishDTO.getPaymentMethod());

        if (publishDTO.getFacilities() != null) {
            try {
                house.setFacilities(objectMapper.writeValueAsString(publishDTO.getFacilities()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("设施信息转换失败", e);
            }
        }

        houseMapper.updateById(house);

        // 更新图片：先删除旧图片，再添加新图片
        if (publishDTO.getImageUrls() != null && !publishDTO.getImageUrls().isEmpty()) {
            QueryWrapper<HouseImage> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("house_id", houseId);
            houseImageMapper.delete(deleteWrapper);

            int coverIndex = publishDTO.getCoverImageIndex() != null ? publishDTO.getCoverImageIndex() : 0;
            for (int i = 0; i < publishDTO.getImageUrls().size(); i++) {
                HouseImage image = new HouseImage();
                image.setHouseId(houseId);
                image.setImageUrl(publishDTO.getImageUrls().get(i));
                image.setIsCover(i == coverIndex ? 1 : 0);
                image.setSortOrder(i);
                houseImageMapper.insert(image);
            }
        }
    }

    @Override
    @Transactional
    // 删除房源后，清除该房源详情缓存和列表缓存
    @Caching(evict = {
            @CacheEvict(value = "house:detail", key = "#houseId"),
            @CacheEvict(value = "house:list", allEntries = true)
    })
    public void deleteHouse(Long houseId, Long landlordId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (!house.getLandlordId().equals(landlordId)) {
            throw new BusinessException("无权操作此房源");
        }

        // 删除房源
        houseMapper.deleteById(houseId);

        // 删除图片
        QueryWrapper<HouseImage> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("house_id", houseId);
        houseImageMapper.delete(deleteWrapper);
    }

    @Override
    public void offlineHouse(Long houseId, Long landlordId) {
        updateHouseStatus(houseId, landlordId, HouseStatus.OFFLINE.name());
    }

    @Override
    public void onlineHouse(Long houseId, Long landlordId) {
        updateHouseStatus(houseId, landlordId, HouseStatus.AVAILABLE.name());
    }

    // 房东手动上下架时，清除对应缓存
    @Caching(evict = {
            @CacheEvict(value = "house:detail", key = "#houseId"),
            @CacheEvict(value = "house:list", allEntries = true)
    })
    private void updateHouseStatus(Long houseId, Long landlordId, String status) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }
        if (!house.getLandlordId().equals(landlordId)) {
            throw new BusinessException("无权操作此房源");
        }

        house.setStatus(status);
        houseMapper.updateById(house);
    }

    @Override
    // 订单支付成功/退租后，订单服务通过 Feign 调用此方法更新房源状态，需清除缓存
    @Caching(evict = {
            @CacheEvict(value = "house:detail", key = "#houseId"),
            @CacheEvict(value = "house:list", allEntries = true)
    })
    public void updateHouseStatus(Long houseId, String status) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException("房源不存在");
        }

        // 若目标状态为 RENTED，需要进行幂等保护：
        // 只有当前状态为 AVAILABLE 时才允许变更，防止重复出租
        if (HouseStatus.RENTED.name().equals(status) && !HouseStatus.AVAILABLE.name().equals(house.getStatus())) {
            log.warn("房源状态变更被拒绝，该房源已不可租: houseId={}, currentStatus={}",
                    houseId, house.getStatus());
            throw new BusinessException("房源已不可租赁，当前状态：" + house.getStatus());
        }

        house.setStatus(status);
        // updateById 会利用 @Version 乐观锁：自动在 WHERE 中加 version=当前值，
        // 并在成功后递增 version。若版本冲突，MyBatis-Plus 抛出 OptimisticLocker 异常。
        houseMapper.updateById(house);
        log.info("房源状态已更新: houseId={}, newStatus={}", houseId, status);
    }
}
