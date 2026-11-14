package com.noval.gateway.domain.model;

/**
 * AI capabilities supported by the gateway.
 */
public enum Capability {
    CHAT,
    VISION,
    IMAGE_GEN;

    public static Capability from(String name) {
        if (name == null) {
            return CHAT;
        }
        for (Capability c : values()) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown capability: " + name);
    }
}
