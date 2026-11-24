package com.noval.workbench.domain.model;

/**
 * 资产实体
 */
public class Asset {
    private final Long id;
    private final Long novelId;
    private final String name;
    private final AssetType type;
    private final ContentStyle style;
    private final String description;
    private final String imageUrl;

    private Asset(Builder builder) {
        this.id = builder.id;
        this.novelId = builder.novelId;
        this.name = builder.name;
        this.type = builder.type;
        this.style = builder.style;
        this.description = builder.description;
        this.imageUrl = builder.imageUrl;
    }

    public Long getId() { return id; }
    public Long getNovelId() { return novelId; }
    public String getName() { return name; }
    public AssetType getType() { return type; }
    public ContentStyle getStyle() { return style; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long novelId;
        private String name;
        private AssetType type;
        private ContentStyle style;
        private String description;
        private String imageUrl;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder novelId(Long novelId) { this.novelId = novelId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(AssetType type) { this.type = type; return this; }
        public Builder style(ContentStyle style) { this.style = style; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Asset build() { return new Asset(this); }
    }
}
