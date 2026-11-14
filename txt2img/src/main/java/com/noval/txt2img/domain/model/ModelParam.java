package com.noval.txt2img.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * A single parameter definition for a model (immutable value object).
 */
public class ModelParam {

    private final String key;
    private final ParamType type;
    private final String defaultValue;
    private final List<String> options;

    public ModelParam(String key, ParamType type, String defaultValue, List<String> options) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(options);
    }

    public ModelParam(String key, ParamType type, String defaultValue) {
        this(key, type, defaultValue, Collections.emptyList());
    }

    public String getKey() { return key; }
    public ParamType getType() { return type; }
    public String getDefaultValue() { return defaultValue; }
    public List<String> getOptions() { return options; }
}