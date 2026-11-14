package com.noval.gateway.domain.model;

/**
 * Supported AI provider types.
 */
public enum ProviderType {
    OPENAI,
    CLAUDE,
    GOOGLE,
    DEEPSEEK,
    LK888;

    public static ProviderType from(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Provider type cannot be null");
        }
        for (ProviderType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown provider type: " + name);
    }
}
