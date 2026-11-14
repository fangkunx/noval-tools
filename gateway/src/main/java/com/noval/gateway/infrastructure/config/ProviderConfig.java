package com.noval.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider configuration loaded from application.yml.
 *
 * gateway:
 *   providers:
 *     openai:
 *       api-key: sk-xxx
 *       base-url: https://api.openai.com
 *     claude:
 *       api-key: sk-ant-xxx
 *       base-url: https://api.anthropic.com
 *     google:
 *       api-key: xxx
 *       base-url: https://generativelanguage.googleapis.com
 */
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class ProviderConfig {

    private Map<String, ProviderSettings> providers = new HashMap<>();

    public Map<String, ProviderSettings> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderSettings> providers) {
        this.providers = providers;
    }

    public ProviderSettings getProvider(String name) {
        return providers.get(name.toLowerCase());
    }

    public static class ProviderSettings {
        private String apiKey;
        private String baseUrl;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
