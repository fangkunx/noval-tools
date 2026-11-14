package com.noval.txt2img.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * Domain command for image generation (immutable, built via Builder).
 * Contains all possible generation parameters; each provider picks what it needs.
 */
public class ImageGenCommand {

    private final String model;
    private final String prompt;
    private final List<String> referenceImages;

    // Common optional params
    private final String size;
    private final String outputFormat;
    private final String quality;
    private final Integer numImages;
    private final String negativePrompt;

    // LK888 specific
    private final String aspectRatio;
    private final String resolution;
    private final String complexity;
    private final String background;
    private final String version;

    private ImageGenCommand(Builder builder) {
        this.model = builder.model;
        this.prompt = builder.prompt;
        this.referenceImages = builder.referenceImages == null
                ? Collections.emptyList() : Collections.unmodifiableList(builder.referenceImages);
        this.size = builder.size;
        this.outputFormat = builder.outputFormat;
        this.quality = builder.quality;
        this.numImages = builder.numImages;
        this.negativePrompt = builder.negativePrompt;
        this.aspectRatio = builder.aspectRatio;
        this.resolution = builder.resolution;
        this.complexity = builder.complexity;
        this.background = builder.background;
        this.version = builder.version;
    }

    public static Builder builder() { return new Builder(); }

    public String getModel() { return model; }
    public String getPrompt() { return prompt; }
    public List<String> getReferenceImages() { return referenceImages; }
    public String getSize() { return size; }
    public String getOutputFormat() { return outputFormat; }
    public String getQuality() { return quality; }
    public Integer getNumImages() { return numImages; }
    public String getNegativePrompt() { return negativePrompt; }
    public String getAspectRatio() { return aspectRatio; }
    public String getResolution() { return resolution; }
    public String getComplexity() { return complexity; }
    public String getBackground() { return background; }
    public String getVersion() { return version; }

    public static class Builder {
        private String model;
        private String prompt;
        private List<String> referenceImages;
        private String size;
        private String outputFormat;
        private String quality;
        private Integer numImages;
        private String negativePrompt;
        private String aspectRatio;
        private String resolution;
        private String complexity;
        private String background;
        private String version;

        public Builder model(String model) { this.model = model; return this; }
        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder referenceImages(List<String> referenceImages) { this.referenceImages = referenceImages; return this; }
        public Builder size(String size) { this.size = size; return this; }
        public Builder outputFormat(String outputFormat) { this.outputFormat = outputFormat; return this; }
        public Builder quality(String quality) { this.quality = quality; return this; }
        public Builder numImages(Integer numImages) { this.numImages = numImages; return this; }
        public Builder negativePrompt(String negativePrompt) { this.negativePrompt = negativePrompt; return this; }
        public Builder aspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; return this; }
        public Builder resolution(String resolution) { this.resolution = resolution; return this; }
        public Builder complexity(String complexity) { this.complexity = complexity; return this; }
        public Builder background(String background) { this.background = background; return this; }
        public Builder version(String version) { this.version = version; return this; }

        public ImageGenCommand build() {
            if (prompt == null || prompt.isEmpty()) throw new IllegalArgumentException("prompt is required");
            return new ImageGenCommand(this);
        }
    }
}