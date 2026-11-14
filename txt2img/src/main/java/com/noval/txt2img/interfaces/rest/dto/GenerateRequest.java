package com.noval.txt2img.interfaces.rest.dto;

import java.util.List;

/**
 * REST DTO for image generation requests.
 * Jackson deserializes into this; assembler maps to domain ImageGenCommand.
 */
public class GenerateRequest {

    private String backend;  // "lk888" | "tokenplan"
    private String model;
    private String prompt;
    private String aspectRatio;
    private String resolution;
    private String quality;
    private String complexity;
    private String outputFormat;
    private String background;
    private String version;
    private String size;
    private List<String> referenceImages;
    private Integer numImages;
    private String negativePrompt;

    public String getBackend() { return backend; }
    public void setBackend(String backend) { this.backend = backend; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public List<String> getReferenceImages() { return referenceImages; }
    public void setReferenceImages(List<String> referenceImages) { this.referenceImages = referenceImages; }
    public Integer getNumImages() { return numImages; }
    public void setNumImages(Integer numImages) { this.numImages = numImages; }
    public String getNegativePrompt() { return negativePrompt; }
    public void setNegativePrompt(String negativePrompt) { this.negativePrompt = negativePrompt; }

    /**
     * Convert this DTO to a domain ImageGenCommand.
     */
    public com.noval.txt2img.domain.model.ImageGenCommand toCommand() {
        return com.noval.txt2img.domain.model.ImageGenCommand.builder()
                .model(model)
                .prompt(prompt)
                .aspectRatio(aspectRatio)
                .resolution(resolution)
                .quality(quality)
                .complexity(complexity)
                .outputFormat(outputFormat)
                .background(background)
                .version(version)
                .size(size)
                .referenceImages(referenceImages)
                .numImages(numImages)
                .negativePrompt(negativePrompt)
                .build();
    }
}