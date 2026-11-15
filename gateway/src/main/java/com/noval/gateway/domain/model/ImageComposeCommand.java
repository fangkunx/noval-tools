package com.noval.gateway.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable image composition command object.
 * Used for combining multiple images into a grid layout.
 */
public class ImageComposeCommand {

    private final String model;
    private final String prompt;
    private final List<String> imageUrls;  // base64 data URLs
    private final String aspectRatio;
    private final String resolution;

    private ImageComposeCommand(Builder builder) {
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.imageUrls = builder.imageUrls == null ? Collections.emptyList()
                : Collections.unmodifiableList(builder.imageUrls);
        this.aspectRatio = builder.aspectRatio;
        this.resolution = builder.resolution;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public String getResolution() {
        return resolution;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private String prompt;
        private List<String> imageUrls;
        private String aspectRatio = "3:4";
        private String resolution = "2K";

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder imageUrls(List<String> imageUrls) {
            this.imageUrls = imageUrls;
            return this;
        }

        public Builder aspectRatio(String aspectRatio) {
            this.aspectRatio = aspectRatio;
            return this;
        }

        public Builder resolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        public ImageComposeCommand build() {
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be empty");
            }
            if (imageUrls == null || imageUrls.isEmpty()) {
                throw new IllegalArgumentException("At least one image is required");
            }
            return new ImageComposeCommand(this);
        }
    }
}
