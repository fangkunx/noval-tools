package com.noval.gateway.infrastructure.provider;

import com.noval.gateway.domain.spi.GatewayProvider;
import com.noval.gateway.infrastructure.config.ProviderConfig;
import com.noval.gateway.infrastructure.logging.ProviderRequestLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that creates Provider beans based on application.yml settings.
 */
@Configuration
public class ProviderBeanConfig {

    @Bean
    public GatewayProvider openAIProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("openai");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://api.openai.com");
        }
        return new OpenAIProvider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider claudeProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("claude");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://api.anthropic.com");
        }
        return new ClaudeProvider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider googleProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("google");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://generativelanguage.googleapis.com");
        }
        return new GoogleProvider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider deepSeekProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("deepseek");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://api.deepseek.com");
        }
        return new DeepSeekProvider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider lk888Provider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("lk888");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://api.lk888.ai");
        }
        return new Lk888Provider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider grsaiProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("grsai");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://grsaiapi.com");
        }
        return new GrsaiProvider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider klChatProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("kl-chat");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://apikl.ai");
        }
        return new KlChatProvider(settings, requestLogger);
    }

    @Bean
    public GatewayProvider klImageProvider(ProviderConfig config, ProviderRequestLogger requestLogger) {
        ProviderConfig.ProviderSettings settings = config.getProvider("kl-image");
        if (settings == null) {
            settings = new ProviderConfig.ProviderSettings();
            settings.setBaseUrl("https://apikl.ai");
        }
        return new KlImageProvider(settings, requestLogger);
    }
}
