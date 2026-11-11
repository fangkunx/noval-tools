package com.noval.txt2img.dto;

public class GenerateRequest {
    private String backend;       // lk888 | tokenplan
    private String model;
    private String prompt;
    private String aspectRatio;   // lk888
    private String resolution;    // lk888
    private String quality;       // lk888
    private String complexity;    // lk888
    private String outputFormat;  // lk888
    private String background;    // lk888
    private String version;       // lk888
    private String size;          // custom size: 1536x864
    private String[] referenceImages; // URLs or base64 data URIs
    private int numImages = 1;    // tokenplan only
    private String negativePrompt;// tokenplan only

    // getters & setters
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
    public String[] getReferenceImages() { return referenceImages; }
    public void setReferenceImages(String[] referenceImages) { this.referenceImages = referenceImages; }
    public int getNumImages() { return numImages; }
    public void setNumImages(int numImages) { this.numImages = numImages; }
    public String getNegativePrompt() { return negativePrompt; }
    public void setNegativePrompt(String negativePrompt) { this.negativePrompt = negativePrompt; }
}