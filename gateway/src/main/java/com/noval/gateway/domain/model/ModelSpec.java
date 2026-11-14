package com.noval.gateway.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Immutable value object representing a model specification.
 */
public class ModelSpec {

    private final String id;
    private final String name;
    private final ProviderType providerType;
    private final Set<Capability> capabilities;

    public ModelSpec(String id, String name, ProviderType providerType, Set<Capability> capabilities) {
        this.id = id;
        this.name = name;
        this.providerType = providerType;
        this.capabilities = Collections.unmodifiableSet(capabilities);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public Set<Capability> getCapabilities() {
        return capabilities;
    }

    public boolean supportsCapability(Capability capability) {
        return capabilities.contains(capability);
    }

    @Override
    public String toString() {
        return "ModelSpec{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", providerType=" + providerType +
                ", capabilities=" + capabilities +
                '}';
    }
}
