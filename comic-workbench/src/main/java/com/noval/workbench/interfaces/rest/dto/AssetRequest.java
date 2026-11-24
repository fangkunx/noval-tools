package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Asset;
import com.noval.workbench.domain.model.AssetType;
import com.noval.workbench.domain.model.ContentStyle;

/**
 * 资产请求 DTO
 */
public class AssetRequest {
    private Long novelId;
    private String name;
    private String type;
    private String style;
    private String description;
    private String imageUrl;

    public Asset toDomain() {
        return Asset.builder()
                .novelId(novelId)
                .name(name)
                .type(type != null ? AssetType.valueOf(type) : null)
                .style(style != null ? ContentStyle.valueOf(style) : null)
                .description(description)
                .imageUrl(imageUrl)
                .build();
    }

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
