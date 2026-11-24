package com.noval.workbench.domain.model;

/**
 * 账号能力枚举
 */
public enum AccountCapability {
    TEXT("文本推理"),
    IMAGE("图像生成");

    private final String label;

    AccountCapability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
