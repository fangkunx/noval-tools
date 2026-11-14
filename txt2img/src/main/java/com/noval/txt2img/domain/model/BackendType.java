package com.noval.txt2img.domain.model;

/**
 * Supported AI image generation backends.
 */
public enum BackendType {
    LK888,
    TOKENPLAN;

    public static BackendType from(String name) {
        if (name == null) return LK888;
        for (BackendType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        throw new IllegalArgumentException("Unknown backend: " + name);
    }
}