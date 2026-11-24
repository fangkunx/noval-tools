package com.noval.workbench.domain.model;

/**
 * 预设类型枚举：角色 / 场景 / 道具
 */
public enum PresetType {
    CHARACTER("角色"),
    SCENE("场景"),
    PROP("道具");

    private final String label;

    PresetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 从字符串解析类型，兼容大小写（character / CHARACTER），无法识别时返回 null
     */
    public static PresetType fromString(String value) {
        if (value == null) return null;
        for (PresetType t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return null;
    }
}
