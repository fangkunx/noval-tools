package com.noval.gateway.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable image generation command object.
 */
public class ImageGenCommand {

    private final String model;
    private final String prompt;
    private final Integer n;
    private final String size;
    private final String responseFormat;

    private ImageGenCommand(Builder builder) {
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.n = builder.n;
        this.size = builder.size;
        this.responseFormat = builder.responseFormat;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public Integer getN() {
        return n;
    }

    public String getSize() {
        return size;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private String prompt;
        private Integer n = 1;
        private String size = "1024x1024";
        private String responseFormat = "url";

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public ImageGenCommand build() {
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be empty");
            }
            return new ImageGenCommand(this);
        }
    }
}
