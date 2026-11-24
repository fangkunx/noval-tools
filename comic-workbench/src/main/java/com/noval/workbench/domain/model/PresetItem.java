package com.noval.workbench.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * 预设条目：角色/场景/道具
 */
public class PresetItem {
    private final String name;
    private final String description;
    private final String type; // CHARACTER / SCENE / PROP
    private final String imageUrl;

    public PresetItem(String name, String description, String type, String imageUrl) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }

    public static PresetItem fromJson(java.util.Map<String, Object> map) {
        return new PresetItem(
                (String) map.get("name"),
                (String) map.get("description"),
                (String) map.get("type"),
                (String) map.get("imageUrl")
        );
    }

    public java.util.Map<String, Object> toJson() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("type", type);
        map.put("imageUrl", imageUrl);
        return map;
    }
}
