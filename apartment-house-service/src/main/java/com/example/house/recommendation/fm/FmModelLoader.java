package com.example.house.recommendation.fm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FM / DeepFM 模型加载器
 * <p>
 * 自动检测 JSON 模型文件格式:
 *   - 仅含 bias/linearWeights/factors -> 纯 FM 模型 (FmModel)
 *   - 额外含 dnnWeights               -> DeepFM 模型 (DeepFmModel, 含 DNN 部分)
 * <p>
 * 训练脚本:
 *   - deploy/recommendation/train_fm_numpy.py (FM, NumPy 实现)
 *   - deploy/recommendation/train_deepfm_numpy.py (DeepFM, NumPy 实现)
 */
@Slf4j
@Component
public class FmModelLoader {

    private static final String DEFAULT_MODEL_PATH = "recommendation/fm-model-v1.json";

    private final ObjectMapper objectMapper;

    /** 训练元数据(version/AUC/trainTime 等),通过 getModelInfo() 暴露给前端 */
    @Getter
    private Map<String, Object> modelInfo;

    /** 当前加载的 DeepFM 模型 (退化为纯 FM 时只有 FM 部分有效) */
    @Getter
    private DeepFmModel loadedDeepFm;

    public FmModelLoader() {
        this.objectMapper = new ObjectMapper();
    }

    public FmModelLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 加载默认模型,返回 DeepFmModel (兼容旧 FM 调用方)
     */
    public DeepFmModel loadDefaultDeepFmModel() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(DEFAULT_MODEL_PATH)) {
            if (inputStream == null) {
                log.warn("FM 模型文件未找到: {},使用默认经验模型", DEFAULT_MODEL_PATH);
                return wrapDefault();
            }
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(inputStream);
            if (root == null || root.isMissingNode()) {
                return wrapDefault();
            }
            if (root.has("metadata")) {
                modelInfo = objectMapper.treeToValue(root.get("metadata"), Map.class);
                log.info("加载 FM/DeepFM 模型: {}", modelInfo);
            }
            DeepFmModel loaded = parseDeepFm(root);
            if (loaded == null
                    || loaded.getLinearWeights() == null || loaded.getLinearWeights().isEmpty()
                    || loaded.getFactors() == null || loaded.getFactors().isEmpty()) {
                log.warn("FM 模型权重为空,fallback 到默认经验模型");
                return wrapDefault();
            }
            this.loadedDeepFm = loaded;
            return loaded;
        } catch (IOException ex) {
            log.error("加载 FM 模型失败: {}", ex.getMessage());
            return wrapDefault();
        }
    }

    /**
     * 兼容旧调用:返回 FmModel 视图 (仅 FM 部分)
     */
    public FmModel loadDefaultModel() {
        DeepFmModel deep = loadDefaultDeepFmModel();
        return new FmModel(deep.getBias(), deep.getLinearWeights(), deep.getFactors());
    }

    private DeepFmModel wrapDefault() {
        FmModel def = FmModel.defaultModel();
        return new DeepFmModel(def.getBias(), def.getLinearWeights(), def.getFactors(),
                null, new double[0], 0.0);
    }

    private DeepFmModel parseDeepFm(com.fasterxml.jackson.databind.JsonNode root) throws IOException {
        DeepFmModel model = new DeepFmModel();
        if (root.has("bias")) {
            model.setBias(root.get("bias").asDouble());
        }
        if (root.has("linearWeights")) {
            Map<String, Double> lw = objectMapper.treeToValue(root.get("linearWeights"), Map.class);
            model.setLinearWeights(lw);
        }
        if (root.has("factors")) {
            Map<String, List<Double>> fac = objectMapper.treeToValue(root.get("factors"), Map.class);
            model.setFactors(fac);
        }
        if (root.has("dnnWeights")) {
            DeepFmModel.DnnWeightsDto dto = objectMapper.treeToValue(root.get("dnnWeights"),
                    DeepFmModel.DnnWeightsDto.class);
            if (dto != null && dto.getLayers() != null && !dto.getLayers().isEmpty()) {
                List<DeepFmModel.DnnLayer> layers = new ArrayList<>(dto.getLayers().size());
                for (DeepFmModel.DnnLayerDto layerDto : dto.getLayers()) {
                    layers.add(layerDto.toLayer());
                }
                model.setDnnLayers(layers);
                if (dto.getOutW() != null) {
                    double[] outW = new double[dto.getOutW().size()];
                    for (int i = 0; i < dto.getOutW().size(); i++) {
                        outW[i] = dto.getOutW().get(i);
                    }
                    model.setOutW(outW);
                }
                model.setOutB(dto.getOutB());
                log.info("DeepFM 模型加载: {} DNN 层, outW 长度 {}",
                        layers.size(), model.getOutW() != null ? model.getOutW().length : 0);
            }
        }
        return model;
    }
}
