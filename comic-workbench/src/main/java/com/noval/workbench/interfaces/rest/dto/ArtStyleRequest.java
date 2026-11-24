package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.ArtStyle;

/**
 * 画风请求 DTO
 */
public class ArtStyleRequest {
    private String name;
    private String description;
    private String imageUrl;

    public ArtStyle toDomain() {
        return ArtStyle.builder()
                .name(name)
                .description(description)
                .imageUrl(imageUrl)
                .usageCount(0)
                .build();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
