package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Account;
import com.noval.workbench.domain.model.AccountCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号请求 DTO
 */
public class AccountRequest {
    private String name;
    private String protocol;
    private String apiUrl;
    private String apiKey;
    private String modelId;
    private String displayName;
    private List<String> capabilities;
    private boolean enabled = true;
    private boolean vip;

    public Account toDomain() {
        List<AccountCapability> caps = capabilities == null
                ? new ArrayList<>()
                : capabilities.stream().map(AccountCapability::valueOf).collect(Collectors.toList());
        return Account.builder()
                .name(name)
                .protocol(protocol)
                .apiUrl(apiUrl)
                .apiKey(apiKey)
                .modelId(modelId)
                .displayName(displayName)
                .capabilities(caps)
                .enabled(enabled)
                .vip(vip)
                .build();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }
}
