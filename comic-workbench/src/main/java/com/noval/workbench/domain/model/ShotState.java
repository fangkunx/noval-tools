package com.noval.workbench.domain.model;

/**
 * 分镜状态枚举
 */
public enum ShotState {
    PENDING("待处理"),
    INFERRING("推理中"),
    INFERRED("已推理"),
    GENERATING("生图中"),
    GENERATED("已生图"),
    FAILED("失败");

    private final String label;

    ShotState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
