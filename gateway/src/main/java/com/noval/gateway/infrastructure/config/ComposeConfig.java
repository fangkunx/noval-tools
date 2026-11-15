package com.noval.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图片合成提示词模板配置
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.compose")
public class ComposeConfig {

    /**
     * 提示词模板，key 为模板名称，value 为提示词内容
     */
    private Map<String, String> promptTemplates = new LinkedHashMap<>();

    public Map<String, String> getPromptTemplates() {
        return promptTemplates;
    }

    public void setPromptTemplates(Map<String, String> promptTemplates) {
        this.promptTemplates = promptTemplates;
    }
}
