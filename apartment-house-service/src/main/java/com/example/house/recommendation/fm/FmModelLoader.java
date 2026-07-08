package com.example.house.recommendation.fm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class FmModelLoader {

    private static final String DEFAULT_MODEL_PATH = "recommendation/fm-model-v1.json";

    private final ObjectMapper objectMapper;

    public FmModelLoader() {
        this(new ObjectMapper());
    }

    public FmModelLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FmModel loadDefaultModel() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(DEFAULT_MODEL_PATH)) {
            if (inputStream == null) {
                return FmModel.defaultModel();
            }
            FmModel loaded = objectMapper.readValue(inputStream, FmModel.class);
            if (loaded.getLinearWeights().isEmpty() || loaded.getFactors().isEmpty()) {
                return FmModel.defaultModel();
            }
            return loaded;
        } catch (IOException ex) {
            return FmModel.defaultModel();
        }
    }
}
