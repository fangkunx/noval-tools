package com.noval.txt2img.interfaces.rest.dto;

import com.noval.txt2img.domain.model.ModelParam;
import com.noval.txt2img.domain.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * REST response DTO for model specifications.
 * Converts domain ModelSpec/ModelParam to frontend-compatible format.
 * Note: options is pipe-delimited String (e.g. "1K|2K|4K") for frontend compatibility.
 */
public class ModelSpecResponse {

    private String name;
    private String displayName;
    private String backend;
    private List<ParamResponse> params;

    public static ModelSpecResponse from(ModelSpec spec) {
        ModelSpecResponse r = new ModelSpecResponse();
        r.name = spec.getName();
        r.displayName = spec.getDisplayName();
        r.backend = spec.getBackend().name().toLowerCase();
        r.params = new ArrayList<>();
        for (ModelParam p : spec.getParams()) {
            r.params.add(ParamResponse.from(p));
        }
        return r;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getBackend() { return backend; }
    public List<ParamResponse> getParams() { return params; }

    public static class ParamResponse {
        private String key;
        private String type;
        private String defaultValue;
        private String options;

        public static ParamResponse from(ModelParam p) {
            ParamResponse r = new ParamResponse();
            r.key = p.getKey();
            r.type = p.getType().name().toLowerCase();
            r.defaultValue = p.getDefaultValue();
            r.options = String.join("|", p.getOptions());
            return r;
        }

        public String getKey() { return key; }
        public String getType() { return type; }
        public String getDefaultValue() { return defaultValue; }
        public String getOptions() { return options; }
    }
}