package com.noval.txt2img.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Specification of an available AI model (immutable value object).
 */
public class ModelSpec {

    private final String name;
    private final String displayName;
    private final BackendType backend;
    private final List<ModelParam> params;

    public ModelSpec(String name, String displayName, BackendType backend, List<ModelParam> params) {
        this.name = name;
        this.displayName = displayName;
        this.backend = backend;
        this.params = params == null ? Collections.emptyList() : Collections.unmodifiableList(params);
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public BackendType getBackend() { return backend; }
    public List<ModelParam> getParams() { return params; }
}