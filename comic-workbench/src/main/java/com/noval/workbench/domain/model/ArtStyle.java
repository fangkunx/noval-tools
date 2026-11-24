package com.noval.workbench.domain.model;

/**
 * 画风实体
 */
public class ArtStyle {
    private final Long id;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final int usageCount;

    private ArtStyle(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.imageUrl = builder.imageUrl;
        this.usageCount = builder.usageCount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getUsageCount() { return usageCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private String imageUrl;
        private int usageCount;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder usageCount(int usageCount) { this.usageCount = usageCount; return this; }
        public ArtStyle build() { return new ArtStyle(this); }
    }
}
