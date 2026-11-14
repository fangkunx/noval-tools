package com.noval.gateway.infrastructure.persistence.entity;

import javax.persistence.*;

/**
 * Capability route configuration entity.
 * Defines which provider/model handles a specific capability.
 */
@Entity
@Table(name = "gateway_capability_route")
public class CapabilityRouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capability", nullable = false, length = 50)
    private String capability;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "weight", nullable = false)
    private Integer weight = 100;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
