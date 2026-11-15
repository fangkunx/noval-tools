package com.noval.gateway.interfaces.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Gateway API")
                        .description("统一 AI 能力代理层 - OpenAI 兼容协议\n\n" +
                                "**支持的 Provider:**\n" +
                                "- **OpenAI**: GPT-4o, DALL-E 3\n" +
                                "- **Claude**: Opus 5.5, Sonnet 5.5, Haiku 5.5\n" +
                                "- **Google Gemini**: 2.0 Flash, 1.5 Pro\n" +
                                "- **DeepSeek**: DeepSeek Chat\n" +
                                "- **LK888**: TT-Image, Banana, GK-Image\n" +
                                "- **Grsai**: Nano Banana 系列图像生成 (nano-banana, nano-banana-2, nano-banana-pro 等)\n\n" +
                                "**Grsai API Base URLs:**\n" +
                                "- Global: https://grsaiapi.com\n" +
                                "- China: https://grsai.dakka.com.cn\n\n" +
                                "**Grsai API Documentation:** https://qmy27nhsd9.apifox.cn/")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Noval Team"))
                        .license(new License()
                                .name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("API Key")
                                        .description("使用 Bearer Token 认证，例如: Bearer gw-default-key-12345")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("gateway")
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public OpenApiCustomiser addGrsaiExamples() {
        return openApi -> {
            // Add Grsai model examples to image generation endpoint
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                if (operation.getOperationId() != null && operation.getOperationId().contains("Image")) {
                    // Already handled via @Schema annotations
                }
            }));
        };
    }
}
