package com.noval.workbench.domain.model;

/**
 * 漫画设置实体：每本小说的推理配置
 */
public class ComicSetting {
    private final Long id;
    private final Long novelId;
    private final String textModelId;
    private final String imageModelId;
    private final String artStyleId;
    private final String prefixPrompt;
    private final String suffixPrompt;
    private final String aspectRatio;

    private ComicSetting(Builder builder) {
        this.id = builder.id;
        this.novelId = builder.novelId;
        this.textModelId = builder.textModelId;
        this.imageModelId = builder.imageModelId;
        this.artStyleId = builder.artStyleId;
        this.prefixPrompt = builder.prefixPrompt;
        this.suffixPrompt = builder.suffixPrompt;
        this.aspectRatio = builder.aspectRatio;
    }

    public Long getId() { return id; }
    public Long getNovelId() { return novelId; }
    public String getTextModelId() { return textModelId; }
    public String getImageModelId() { return imageModelId; }
    public String getArtStyleId() { return artStyleId; }
    public String getPrefixPrompt() { return prefixPrompt; }
    public String getSuffixPrompt() { return suffixPrompt; }
    public String getAspectRatio() { return aspectRatio; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long novelId;
        private String textModelId;
        private String imageModelId;
        private String artStyleId;
        private String prefixPrompt;
        private String suffixPrompt;
        private String aspectRatio;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder novelId(Long novelId) { this.novelId = novelId; return this; }
        public Builder textModelId(String textModelId) { this.textModelId = textModelId; return this; }
        public Builder imageModelId(String imageModelId) { this.imageModelId = imageModelId; return this; }
        public Builder artStyleId(String artStyleId) { this.artStyleId = artStyleId; return this; }
        public Builder prefixPrompt(String prefixPrompt) { this.prefixPrompt = prefixPrompt; return this; }
        public Builder suffixPrompt(String suffixPrompt) { this.suffixPrompt = suffixPrompt; return this; }
        public Builder aspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; return this; }
        public ComicSetting build() { return new ComicSetting(this); }
    }
}
