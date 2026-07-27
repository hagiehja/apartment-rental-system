package com.example.house.recommendation.fm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * DeepFM 评分器 (Java 端推理)
 * <p>
 * FM 输出 + DNN 输出, 最后 sigmoid.
 * 当模型不含 DNN 权重时退化为纯 FM 评分.
 *
 * <pre>
 *   fm_logit  = bias + sum(w_i x_i) + 0.5*sum_k[(sum_i V_ik x_i)^2 - sum_i V_ik^2 x_i^2]
 *   h1        = ReLU(W1 x + b1)
 *   h2        = ReLU(W2 h1 + b2)
 *   h3        = ReLU(W3 h2 + b3)
 *   dnn_logit = outW . h3 + outB
 *   final     = sigmoid(fm_logit + dnn_logit)
 * </pre>
 */
public class DeepFmScorer {

    private final FmModel fmPart;       // 复用 FmModel 存 FM 权重
    private final DeepFmModel deepPart; // DNN 部分 (可能为空)

    public DeepFmScorer(DeepFmModel model) {
        if (model == null) {
            this.fmPart = FmModel.defaultModel();
            this.deepPart = null;
        } else {
            // Wrap as FmModel for reuse
            FmModel fm = new FmModel(model.getBias(), model.getLinearWeights(), model.getFactors());
            this.fmPart = fm;
            this.deepPart = model.hasDnn() ? model : null;
        }
    }

    /** 兼容旧 FM 模型 */
    public DeepFmScorer(FmModel fmModel) {
        this.fmPart = fmModel == null ? FmModel.defaultModel() : fmModel;
        this.deepPart = null;
    }

    public double scoreProbability(FmFeatureVector vector) {
        FmFeatureVector safeVector = vector == null ? FmFeatureVector.empty() : vector;
        double fmLogit = computeFmLogit(safeVector);
        double dnnLogit = (deepPart != null) ? computeDnnLogit(safeVector) : 0.0;
        return sigmoid(fmLogit + dnnLogit);
    }

    public double score100(FmFeatureVector vector) {
        return round(scoreProbability(vector) * 100.0);
    }

    private double computeFmLogit(FmFeatureVector vector) {
        double linear = 0.0;
        for (Map.Entry<String, Double> e : vector.values().entrySet()) {
            linear += fmPart.getLinearWeights().getOrDefault(e.getKey(), 0.0) * e.getValue();
        }
        int factorSize = fmPart.getFactors().values().stream()
                .mapToInt(List::size).max().orElse(0);
        double cross = 0.0;
        for (int f = 0; f < factorSize; f++) {
            double summed = 0.0;
            double squaredSum = 0.0;
            for (Map.Entry<String, Double> e : vector.values().entrySet()) {
                List<Double> fac = fmPart.getFactors().get(e.getKey());
                if (fac == null || f >= fac.size()) continue;
                double vx = fac.get(f) * e.getValue();
                summed += vx;
                squaredSum += vx * vx;
            }
            cross += 0.5 * ((summed * summed) - squaredSum);
        }
        return fmPart.getBias() + linear + cross;
    }

    private double computeDnnLogit(FmFeatureVector vector) {
        // Convert sparse feature vector to dense double[] aligned with FmFeatureBuilder order.
        // We rely on DeepFmModel.linearWeights preserving insertion order matching training-time
        // FEATURE_NAMES. Java FmFeatureBuilder uses the same keys, so we can iterate.
        Map<String, Double> weights = deepPart.getLinearWeights();
        String[] featNames = weights.keySet().toArray(new String[0]);
        int n = featNames.length;
        double[] a = new double[n];
        for (int i = 0; i < n; i++) {
            a[i] = vector.values().getOrDefault(featNames[i], 0.0);
        }
        // Forward through hidden layers with ReLU
        for (DeepFmModel.DnnLayer layer : deepPart.getDnnLayers()) {
            double[][] W = layer.getW();
            double[] b = layer.getB();
            int in = W.length;
            int out = W[0].length;
            double[] z = new double[out];
            for (int j = 0; j < out; j++) {
                double s = b[j];
                for (int i = 0; i < in; i++) {
                    s += W[i][j] * a[i];
                }
                z[j] = s;
            }
            // ReLU + produce next 'a' with new length = out
            double[] nextA = new double[out];
            for (int j = 0; j < out; j++) {
                nextA[j] = z[j] > 0 ? z[j] : 0.0;
            }
            a = nextA;
        }
        // Output layer
        double[] outW = deepPart.getOutW();
        double logit = deepPart.getOutB();
        for (int i = 0; i < outW.length && i < a.length; i++) {
            logit += outW[i] * a[i];
        }
        return logit;
    }

    private double sigmoid(double v) {
        if (v >= 35.0) return 1.0;
        if (v <= -35.0) return 0.0;
        return 1.0 / (1.0 + Math.exp(-v));
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public boolean hasDnn() {
        return deepPart != null && deepPart.hasDnn();
    }
}