package com.example.house.recommendation.fm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FmModel {

    private double bias;
    private Map<String, Double> linearWeights = new LinkedHashMap<>();
    private Map<String, List<Double>> factors = new LinkedHashMap<>();

    public FmModel() {
    }

    public FmModel(double bias, Map<String, Double> linearWeights, Map<String, List<Double>> factors) {
        this.bias = bias;
        this.linearWeights = new LinkedHashMap<>(linearWeights == null ? Map.of() : linearWeights);
        this.factors = new LinkedHashMap<>(factors == null ? Map.of() : factors);
    }

    public static FmModel defaultModel() {
        return new FmModel(
                -1.15,
                Map.ofEntries(
                        Map.entry("match.city", 0.55),
                        Map.entry("match.district", 1.05),
                        Map.entry("match.rentType", 0.45),
                        Map.entry("price.inRange", 0.85),
                        Map.entry("price.nearRange", 0.30),
                        Map.entry("price.bucket.mid", 0.18),
                        Map.entry("room.exact", 0.65),
                        Map.entry("room.near", 0.25),
                        Map.entry("status.available", 0.75),
                        Map.entry("status.rented", -0.25),
                        Map.entry("popularity.high", 0.35),
                        Map.entry("popularity.medium", 0.15),
                        Map.entry("freshness.week", 0.40),
                        Map.entry("freshness.month", 0.18)
                ),
                Map.ofEntries(
                        Map.entry("match.city", List.of(0.35, 0.05, 0.12)),
                        Map.entry("match.district", List.of(0.70, 0.12, 0.18)),
                        Map.entry("match.rentType", List.of(0.22, 0.45, 0.10)),
                        Map.entry("price.inRange", List.of(0.62, 0.15, 0.20)),
                        Map.entry("price.nearRange", List.of(0.28, 0.06, 0.08)),
                        Map.entry("price.bucket.mid", List.of(0.12, 0.05, 0.04)),
                        Map.entry("room.exact", List.of(0.18, 0.55, 0.16)),
                        Map.entry("room.near", List.of(0.08, 0.28, 0.08)),
                        Map.entry("status.available", List.of(0.30, 0.18, 0.48)),
                        Map.entry("status.rented", List.of(-0.08, -0.05, -0.10)),
                        Map.entry("popularity.high", List.of(0.20, 0.10, 0.35)),
                        Map.entry("popularity.medium", List.of(0.08, 0.04, 0.14)),
                        Map.entry("freshness.week", List.of(0.24, 0.08, 0.32)),
                        Map.entry("freshness.month", List.of(0.12, 0.04, 0.16))
                )
        );
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public Map<String, Double> getLinearWeights() {
        return linearWeights;
    }

    public void setLinearWeights(Map<String, Double> linearWeights) {
        this.linearWeights = new LinkedHashMap<>(linearWeights == null ? Map.of() : linearWeights);
    }

    public Map<String, List<Double>> getFactors() {
        return factors;
    }

    public void setFactors(Map<String, List<Double>> factors) {
        this.factors = new LinkedHashMap<>(factors == null ? Map.of() : factors);
    }
}