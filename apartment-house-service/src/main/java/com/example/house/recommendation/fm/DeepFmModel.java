package com.example.house.recommendation.fm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepFM 模型权重
 * <p>
 * DeepFM = FM(bias + linear + 2-order) + DNN(high-order).
 * 当 dnnLayers 为空时退化为纯 FM。
 */
public class DeepFmModel {

    private double bias;
    private Map<String, Double> linearWeights = new LinkedHashMap<>();
    private Map<String, List<Double>> factors = new LinkedHashMap<>();
    /** DNN hidden layers, 每层 W (in×out 行主序存储) + b */
    private List<DnnLayer> dnnLayers = new ArrayList<>();
    /** 输出层权重 + bias */
    private double[] outW;
    private double outB;

    public static class DnnLayer {
        private final double[][] W;   // [in][out]
        private final double[] b;     // [out]
        public DnnLayer(double[][] W, double[] b) {
            this.W = W; this.b = b;
        }
        public double[][] getW() { return W; }
        public double[] getB() { return b; }
    }

    public DeepFmModel() {}

    public DeepFmModel(double bias, Map<String, Double> linearWeights,
                       Map<String, List<Double>> factors,
                       List<DnnLayer> dnnLayers, double[] outW, double outB) {
        this.bias = bias;
        this.linearWeights = new LinkedHashMap<>(linearWeights == null ? Map.of() : linearWeights);
        this.factors = new LinkedHashMap<>(factors == null ? Map.of() : factors);
        this.dnnLayers = dnnLayers == null ? new ArrayList<>() : new ArrayList<>(dnnLayers);
        this.outW = outW == null ? new double[0] : outW.clone();
        this.outB = outB;
    }

    public double getBias() { return bias; }
    public void setBias(double bias) { this.bias = bias; }
    public Map<String, Double> getLinearWeights() { return linearWeights; }
    public void setLinearWeights(Map<String, Double> w) {
        this.linearWeights = new LinkedHashMap<>(w == null ? Map.of() : w);
    }
    public Map<String, List<Double>> getFactors() { return factors; }
    public void setFactors(Map<String, List<Double>> f) {
        this.factors = new LinkedHashMap<>(f == null ? Map.of() : f);
    }
    public List<DnnLayer> getDnnLayers() { return dnnLayers; }
    public void setDnnLayers(List<DnnLayer> layers) {
        this.dnnLayers = layers == null ? new ArrayList<>() : layers;
    }
    public double[] getOutW() { return outW; }
    public void setOutW(double[] outW) { this.outW = outW == null ? new double[0] : outW.clone(); }
    public double getOutB() { return outB; }
    public void setOutB(double outB) { this.outB = outB; }

    /** 是否为真正的 DeepFM（含 DNN 权重） */
    public boolean hasDnn() {
        return dnnLayers != null && !dnnLayers.isEmpty()
                && outW != null && outW.length > 0;
    }

    /** DNN 权重 JSON 反序列化辅助结构 */
    public static class DnnLayerDto {
        @com.fasterxml.jackson.annotation.JsonProperty("W")
        private List<List<Double>> W;
        @com.fasterxml.jackson.annotation.JsonProperty("b")
        private List<Double> b;
        @com.fasterxml.jackson.annotation.JsonProperty("W")
        public List<List<Double>> getW() { return W; }
        @com.fasterxml.jackson.annotation.JsonProperty("W")
        public void setW(List<List<Double>> W) { this.W = W; }
        @com.fasterxml.jackson.annotation.JsonProperty("b")
        public List<Double> getB() { return b; }
        @com.fasterxml.jackson.annotation.JsonProperty("b")
        public void setB(List<Double> b) { this.b = b; }
        public DnnLayer toLayer() {
            int in = W.size();
            int out = W.get(0).size();
            double[][] Warr = new double[in][out];
            for (int i = 0; i < in; i++) {
                List<Double> row = W.get(i);
                for (int j = 0; j < out; j++) Warr[i][j] = row.get(j);
            }
            double[] barr = new double[b.size()];
            for (int i = 0; i < b.size(); i++) barr[i] = b.get(i);
            return new DnnLayer(Warr, barr);
        }
    }

    public static class DnnWeightsDto {
        private List<DnnLayerDto> layers;
        @com.fasterxml.jackson.annotation.JsonProperty("outW")
        private List<Double> outW;
        @com.fasterxml.jackson.annotation.JsonProperty("outB")
        private double outB;
        public List<DnnLayerDto> getLayers() { return layers; }
        public void setLayers(List<DnnLayerDto> layers) { this.layers = layers; }
        @com.fasterxml.jackson.annotation.JsonProperty("outW")
        public List<Double> getOutW() { return outW; }
        @com.fasterxml.jackson.annotation.JsonProperty("outW")
        public void setOutW(List<Double> outW) { this.outW = outW; }
        @com.fasterxml.jackson.annotation.JsonProperty("outB")
        public double getOutB() { return outB; }
        @com.fasterxml.jackson.annotation.JsonProperty("outB")
        public void setOutB(double outB) { this.outB = outB; }
    }
}