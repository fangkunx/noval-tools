package com.noval.workbench.domain.model;

import java.util.Collections;
import java.util.List;

/**
 * 账号实体 - 账号池聚合根
 */
public class Account {
    private final Long id;
    private final String name;
    private final String protocol;
    private final String apiUrl;
    private final String apiKey;
    private final String modelId;
    private final String displayName;
    private final List<AccountCapability> capabilities;
    private final boolean enabled;
    private final boolean vip;

    private Account(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.protocol = builder.protocol;
        this.apiUrl = builder.apiUrl;
        this.apiKey = builder.apiKey;
        this.modelId = builder.modelId;
        this.displayName = builder.displayName;
        this.capabilities = builder.capabilities == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(builder.capabilities);
        this.enabled = builder.enabled;
        this.vip = builder.vip;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProtocol() { return protocol; }
    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
    public String getModelId() { return modelId; }
    public String getDisplayName() { return displayName; }
    public List<AccountCapability> getCapabilities() { return capabilities; }
    public boolean isEnabled() { return enabled; }
    public boolean isVip() { return vip; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String name;
        private String protocol;
        private String apiUrl;
        private String apiKey;
        private String modelId;
        private String displayName;
        private List<AccountCapability> capabilities;
        private boolean enabled = true;
        private boolean vip = false;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder protocol(String protocol) { this.protocol = protocol; return this; }
        public Builder apiUrl(String apiUrl) { this.apiUrl = apiUrl; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder capabilities(List<AccountCapability> capabilities) { this.capabilities = capabilities; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder vip(boolean vip) { this.vip = vip; return this; }
        public Account build() { return new Account(this); }
    }
}
