package com.noval.gateway.domain.model;

/**
 * Immutable image edit command object (image-to-image).
 */
public class ImageEditCommand {

    private final String model;
    private final String prompt;
    private final String image;        // Base64 encoded image or URL
    private final String mask;         // Base64 encoded mask or URL (optional)
    private final Integer n;
    private final String size;
    private final String responseFormat;
    private final Float strength;      // Image edit strength (0.0-1.0)

    private ImageEditCommand(Builder builder) {
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.image = builder.image;
        this.mask = builder.mask;
        this.n = builder.n;
        this.size = builder.size;
        this.responseFormat = builder.responseFormat;
        this.strength = builder.strength;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getImage() {
        return image;
    }

    public String getMask() {
        return mask;
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

    public Float getStrength() {
        return strength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private String prompt;
        private String image;
        private String mask;
        private Integer n = 1;
        private String size;  // 默认为 null，由 Provider 根据模型设置默认值
        private String responseFormat = "url";
        private Float strength = 0.75f;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder mask(String mask) {
            this.mask = mask;
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

        public Builder strength(Float strength) {
            this.strength = strength;
            return this;
        }

        public ImageEditCommand build() {
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be empty");
            }
            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("Image cannot be empty");
            }
            return new ImageEditCommand(this);
        }
    }
}
