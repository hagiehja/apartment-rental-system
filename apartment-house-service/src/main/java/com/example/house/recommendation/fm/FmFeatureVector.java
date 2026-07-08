package com.example.house.recommendation.fm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FmFeatureVector {

    private final Map<String, Double> values;

    private FmFeatureVector(Map<String, Double> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static FmFeatureVector of(Map<String, Double> values) {
        return new FmFeatureVector(values == null ? Map.of() : values);
    }

    public static FmFeatureVector empty() {
        return new FmFeatureVector(Map.of());
    }

    public double value(String featureName) {
        return values.getOrDefault(featureName, 0.0);
    }

    public Map<String, Double> values() {
        return values;
    }
}
