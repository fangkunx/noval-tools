package com.noval.workbench.domain.model;

/**
 * 技术标准/协议枚举
 */
public enum TechProtocol {
    OPENAI("openai 协议"),
    GEMINI("gemini 协议"),
    ANTHROPIC("anthropic 协议"),
    CUSTOM("自定义探测");

    private final String label;

    TechProtocol(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
