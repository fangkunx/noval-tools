package com.noval.workbench.interfaces.rest.dto;

import com.noval.workbench.domain.model.Account;
import com.noval.workbench.domain.model.AccountCapability;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号响应 DTO
 */
public class AccountResponse {
    private Long id;
    private String name;
    private String protocol;
    private String apiUrl;
    private String modelId;
    private String displayName;
    private List<String> capabilities;
    private boolean enabled;
    private boolean vip;

    public static AccountResponse from(Account account) {
        AccountResponse resp = new AccountResponse();
        resp.id = account.getId();
        resp.name = account.getName();
        resp.protocol = account.getProtocol();
        resp.apiUrl = account.getApiUrl();
        resp.modelId = account.getModelId();
        resp.displayName = account.getDisplayName();
        resp.capabilities = account.getCapabilities().stream()
                .map(AccountCapability::name)
                .collect(Collectors.toList());
        resp.enabled = account.isEnabled();
        resp.vip = account.isVip();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
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
