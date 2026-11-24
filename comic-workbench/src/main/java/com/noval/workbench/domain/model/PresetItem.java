package com.noval.workbench.domain.model;

/**
 * 预设条目：角色/场景/道具
 */
public class PresetItem {
    private final String name;
    private final String description;
    private final PresetType type;
    private final String imageUrl;

    public PresetItem(String name, String description, PresetType type, String imageUrl) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public PresetType getType() { return type; }
    public String getImageUrl() { return imageUrl; }

    public static PresetItem fromJson(java.util.Map<String, Object> map) {
        return new PresetItem(
                (String) map.get("name"),
                (String) map.get("description"),
                PresetType.fromString((String) map.get("type")),
                (String) map.get("imageUrl")
        );
    }

    public java.util.Map<String, Object> toJson() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("type", type != null ? type.name() : null);
        map.put("imageUrl", imageUrl);
        return map;
    }
}
