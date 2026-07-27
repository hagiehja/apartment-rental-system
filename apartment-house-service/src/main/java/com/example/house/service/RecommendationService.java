package com.example.house.service;

import com.example.house.dto.BehaviorTrackDTO;
import com.example.house.dto.HouseRecommendDTO;
import com.example.house.dto.UserPreferenceDTO;
import com.example.house.model.PageResult;

public interface RecommendationService {

    void trackBehavior(BehaviorTrackDTO trackDTO, Long headerUserId);

    void savePreference(UserPreferenceDTO preferenceDTO, Long headerUserId);

    PageResult<HouseRecommendDTO> recommend(Long userId, Integer pageNum, Integer pageSize);

    UserPreferenceDTO getPreference(Long userId);
}
