package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Asset;
import com.noval.workbench.domain.model.AssetType;
import com.noval.workbench.domain.model.ContentStyle;

/**
 * 资产响应 DTO
 */
public class AssetResponse {
    private Long id;
    private Long novelId;
    private String name;
    private String type;
    private String style;
    private String description;
    private String imageUrl;

    public static AssetResponse from(Asset asset) {
        AssetResponse resp = new AssetResponse();
        resp.id = asset.getId();
        resp.novelId = asset.getNovelId();
        resp.name = asset.getName();
        resp.type = asset.getType() != null ? asset.getType().name() : null;
        resp.style = asset.getStyle() != null ? asset.getStyle().name() : null;
        resp.description = asset.getDescription();
        resp.imageUrl = asset.getImageUrl();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNovelId() { return novelId; }
    public void setNovelId(Long novelId) { this.novelId = novelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
