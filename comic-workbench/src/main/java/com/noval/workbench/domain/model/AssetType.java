package com.noval.workbench.domain.model;

/**
 * 资产类型枚举
 */
public enum AssetType {
    CHARACTER("角色"),
    SCENE("场景"),
    PROP("道具");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
