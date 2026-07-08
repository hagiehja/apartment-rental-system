package com.example.house.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.house.dto.BehaviorTrackDTO;
import com.example.house.dto.HouseRecommendDTO;
import com.example.house.dto.UserPreferenceDTO;
import com.example.house.entity.House;
import com.example.house.entity.HouseImage;
import com.example.house.entity.UserBehavior;
import com.example.house.entity.UserPreference;
import com.example.house.mapper.HouseImageMapper;
import com.example.house.mapper.HouseMapper;
import com.example.house.mapper.UserBehaviorMapper;
import com.example.house.mapper.UserPreferenceMapper;
import com.example.house.model.PageResult;
import com.example.house.recommendation.RecommendationCandidate;
import com.example.house.recommendation.RecommendationScore;
import com.example.house.recommendation.fm.HybridRecommendationScorer;
import com.example.house.recommendation.UserPreferenceSnapshot;
import com.example.house.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int MAX_CANDIDATE_SIZE = 200;

    private final HouseMapper houseMapper;
    private final HouseImageMapper houseImageMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final HybridRecommendationScorer hybridRecommendationScorer;

    @Override
    @Transactional
    public void trackBehavior(BehaviorTrackDTO trackDTO, Long headerUserId) {
        Long userId = resolveUserId(trackDTO.getUserId(), headerUserId);
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setHouseId(trackDTO.getHouseId());
        behavior.setBehaviorType(normalizeBehavior(trackDTO.getBehaviorType()));
        behavior.setScore(behaviorScore(behavior.getBehaviorType()));
        behavior.setSource(trackDTO.getSource());
        behavior.setCreateTime(LocalDateTime.now());
        userBehaviorMapper.insert(behavior);
    }

    @Override
    @Transactional
    public void savePreference(UserPreferenceDTO preferenceDTO, Long headerUserId) {
        Long userId = resolveUserId(preferenceDTO.getUserId(), headerUserId);
        UserPreference existing = userPreferenceMapper.selectById(userId);
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setCity(preferenceDTO.getCity());
        preference.setDistrict(preferenceDTO.getDistrict());
        preference.setMinPrice(preferenceDTO.getMinPrice());
        preference.setMaxPrice(preferenceDTO.getMaxPrice());
        preference.setRoomCount(preferenceDTO.getRoomCount());
        preference.setMinArea(preferenceDTO.getMinArea());
        preference.setMaxArea(preferenceDTO.getMaxArea());
        preference.setRentType(preferenceDTO.getRentType());
        preference.setCommuteAddress(preferenceDTO.getCommuteAddress());
        preference.setUpdateTime(LocalDateTime.now());
        if (existing == null) {
            preference.setCreateTime(LocalDateTime.now());
            userPreferenceMapper.insert(preference);
        } else {
            preference.setCreateTime(existing.getCreateTime());
            userPreferenceMapper.updateById(preference);
        }
    }

    @Override
    public PageResult<HouseRecommendDTO> recommend(Long userId, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        UserPreference preference = userId == null ? null : userPreferenceMapper.selectById(userId);
        UserPreferenceSnapshot snapshot = toSnapshot(preference);

        QueryWrapper<House> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("status", "AVAILABLE", "RENTED");
        if (preference != null) {
            if (StringUtils.hasText(preference.getCity())) {
                queryWrapper.eq("city", preference.getCity());
            }
            if (preference.getMaxPrice() != null) {
                queryWrapper.le("price", preference.getMaxPrice().multiply(new java.math.BigDecimal("1.30")));
            }
        }
        queryWrapper.orderByDesc("view_count").orderByDesc("create_time");
        Page<House> candidatePage = new Page<>(1, MAX_CANDIDATE_SIZE);
        List<House> candidates = houseMapper.selectPage(candidatePage, queryWrapper).getRecords();

        List<ScoredHouse> scored = candidates.stream()
                .map(house -> new ScoredHouse(house, hybridRecommendationScorer.score(snapshot, toCandidate(house))))
                .sorted(Comparator.comparingDouble((ScoredHouse item) -> item.score.getScore()).reversed()
                        .thenComparing(item -> item.house.getCreateTime(), Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = scored.size();
        int fromIndex = Math.min((currentPage - 1) * currentSize, scored.size());
        int toIndex = Math.min(fromIndex + currentSize, scored.size());
        List<House> pageHouses = scored.subList(fromIndex, toIndex).stream()
                .map(item -> item.house)
                .collect(Collectors.toList());
        Map<Long, String> coverImages = loadCoverImages(pageHouses);
        List<HouseRecommendDTO> result = scored.subList(fromIndex, toIndex).stream()
                .map(item -> toRecommendDTO(item.house, item.score, coverImages.get(item.house.getHouseId())))
                .collect(Collectors.toList());

        return new PageResult<>(total, currentPage, currentSize, result);
    }

    private Long resolveUserId(Long bodyUserId, Long headerUserId) {
        Long userId = headerUserId != null ? headerUserId : bodyUserId;
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId;
    }

    private String normalizeBehavior(String behaviorType) {
        if (!StringUtils.hasText(behaviorType)) {
            return "VIEW";
        }
        return behaviorType.trim().toUpperCase();
    }

    private int behaviorScore(String behaviorType) {
        return switch (behaviorType) {
            case "PAY" -> 10;
            case "ORDER" -> 8;
            case "FAVORITE" -> 4;
            case "CLICK" -> 2;
            default -> 1;
        };
    }

    private UserPreferenceSnapshot toSnapshot(UserPreference preference) {
        if (preference == null) {
            return UserPreferenceSnapshot.builder().build();
        }
        return UserPreferenceSnapshot.builder()
                .city(preference.getCity())
                .district(preference.getDistrict())
                .minPrice(preference.getMinPrice())
                .maxPrice(preference.getMaxPrice())
                .roomCount(preference.getRoomCount())
                .minArea(preference.getMinArea())
                .maxArea(preference.getMaxArea())
                .rentType(preference.getRentType())
                .build();
    }

    private RecommendationCandidate toCandidate(House house) {
        return RecommendationCandidate.builder()
                .houseId(house.getHouseId())
                .city(house.getCity())
                .district(house.getDistrict())
                .price(house.getPrice())
                .roomCount(house.getRoomCount())
                .area(house.getArea())
                .rentType(house.getRentType())
                .viewCount(house.getViewCount())
                .status(house.getStatus())
                .createTime(house.getCreateTime())
                .build();
    }

    private Map<Long, String> loadCoverImages(List<House> houses) {
        List<Long> houseIds = houses.stream()
                .map(House::getHouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (houseIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<HouseImage> imageWrapper = new QueryWrapper<>();
        imageWrapper.in("house_id", houseIds).eq("is_cover", 1);
        return houseImageMapper.selectList(imageWrapper).stream()
                .collect(Collectors.toMap(HouseImage::getHouseId, HouseImage::getImageUrl, (left, right) -> left));
    }

    private HouseRecommendDTO toRecommendDTO(House house, RecommendationScore score, String coverImage) {
        HouseRecommendDTO dto = new HouseRecommendDTO();
        dto.setHouseId(house.getHouseId());
        dto.setTitle(house.getTitle());
        dto.setCity(house.getCity());
        dto.setDistrict(house.getDistrict());
        dto.setArea(house.getArea());
        dto.setRoomCount(house.getRoomCount());
        dto.setHallCount(house.getHallCount());
        dto.setRentType(house.getRentType());
        dto.setPrice(house.getPrice());
        dto.setCoverImage(coverImage);
        dto.setViewCount(house.getViewCount());
        dto.setStatus(house.getStatus());
        dto.setCreateTime(house.getCreateTime());
        dto.setRecommendScore(score.getScore());
        dto.setRecommendReason(score.getReason());
        return dto;
    }

    private record ScoredHouse(House house, RecommendationScore score) {
    }
}
