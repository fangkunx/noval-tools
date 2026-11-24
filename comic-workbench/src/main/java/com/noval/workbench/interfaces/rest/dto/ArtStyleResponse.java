package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.ArtStyle;

/**
 * 画风响应 DTO
 */
public class ArtStyleResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private int usageCount;

    public static ArtStyleResponse from(ArtStyle artStyle) {
        ArtStyleResponse resp = new ArtStyleResponse();
        resp.id = artStyle.getId();
        resp.name = artStyle.getName();
        resp.description = artStyle.getDescription();
        resp.imageUrl = artStyle.getImageUrl();
        resp.usageCount = artStyle.getUsageCount();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
}
