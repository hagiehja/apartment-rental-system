package com.example.house.recommendation.fm;

import com.example.house.recommendation.RecommendationCandidate;
import com.example.house.recommendation.UserPreferenceSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FmFeatureBuilderTest {

    @Test
    void buildsMatchAndBucketFeaturesFromPreferenceAndHouse() {
        FmFeatureBuilder builder = new FmFeatureBuilder();
        UserPreferenceSnapshot preference = UserPreferenceSnapshot.builder()
                .city("Shanghai")
                .district("Pudong")
                .minPrice(new BigDecimal("3000"))
                .maxPrice(new BigDecimal("5000"))
                .roomCount(2)
                .rentType("WHOLE")
                .build();
        RecommendationCandidate house = RecommendationCandidate.builder()
                .city("Shanghai")
                .district("Pudong")
                .price(new BigDecimal("4200"))
                .roomCount(2)
                .rentType("WHOLE")
                .viewCount(320)
                .status("AVAILABLE")
                .createTime(LocalDateTime.now().minusDays(3))
                .build();

        FmFeatureVector vector = builder.build(preference, house);

        assertEquals(1.0, vector.value("match.city"));
        assertEquals(1.0, vector.value("match.district"));
        assertEquals(1.0, vector.value("match.rentType"));
        assertEquals(1.0, vector.value("price.inRange"));
        assertEquals(1.0, vector.value("room.exact"));
        assertEquals(1.0, vector.value("status.available"));
        assertEquals(1.0, vector.value("popularity.high"));
        assertEquals(1.0, vector.value("freshness.week"));
    }

    @Test
    void marksNearRangeAndNearRoomWhenHouseIsCloseToPreference() {
        FmFeatureBuilder builder = new FmFeatureBuilder();
        UserPreferenceSnapshot preference = UserPreferenceSnapshot.builder()
                .minPrice(new BigDecimal("3000"))
                .maxPrice(new BigDecimal("5000"))
                .roomCount(2)
                .build();
        RecommendationCandidate house = RecommendationCandidate.builder()
                .price(new BigDecimal("5500"))
                .roomCount(3)
                .status("RENTED")
                .viewCount(80)
                .createTime(LocalDateTime.now().minusDays(20))
                .build();

        FmFeatureVector vector = builder.build(preference, house);

        assertEquals(1.0, vector.value("price.nearRange"));
        assertEquals(1.0, vector.value("room.near"));
        assertEquals(1.0, vector.value("status.rented"));
        assertEquals(1.0, vector.value("popularity.medium"));
        assertEquals(1.0, vector.value("freshness.month"));
    }
}
