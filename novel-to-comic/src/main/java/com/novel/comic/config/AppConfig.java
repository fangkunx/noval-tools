package com.novel.comic.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public PipelineConfig pipelineConfig() {
        PipelineConfig cfg = new PipelineConfig();

        // Try loading from config.json (Node-compatible), fall back to defaults
        try {
            // Check beside JAR/workdir first, then classpath
            File localFile = new File("config.json");
            InputStream is;
            if (localFile.exists()) {
                is = new FileInputStream(localFile);
            } else {
                is = new ClassPathResource("config.json").getInputStream();
            }
            JsonNode root = new ObjectMapper().readTree(is);
            is.close();

            JsonNode api = root.has("api") ? root.get("api") : (root.has("anthropic") ? root.get("anthropic") : null);
            if (api != null) {
                if (api.has("apiKey") && !api.get("apiKey").asText().isEmpty())
                    cfg.apiKey = api.get("apiKey").asText();
                if (api.has("baseUrl")) cfg.baseUrl = api.get("baseUrl").asText();
                if (api.has("model")) cfg.model = api.get("model").asText();
                if (api.has("maxTokens")) cfg.maxTokens = api.get("maxTokens").asInt();
                if (api.has("temperature")) cfg.temperature = api.get("temperature").asDouble();
                if (api.has("concurrency")) cfg.concurrency = api.get("concurrency").asInt();
            }
            if (root.has("server")) {
                JsonNode srv = root.get("server");
                if (srv.has("port")) cfg.port = srv.get("port").asInt();
                if (srv.has("host")) cfg.host = srv.get("host").asText();
            }
            if (root.has("pipeline")) {
                JsonNode pl = root.get("pipeline");
                if (pl.has("maxSegments")) cfg.maxSegments = pl.get("maxSegments").asInt();
                if (pl.has("defaultSegments")) cfg.defaultSegments = pl.get("defaultSegments").asInt();
            }
            if (root.has("output") && root.get("output").has("dir")) {
                cfg.outputDir = root.get("output").get("dir").asText();
            }
        } catch (Exception e) {
            System.out.println("[CONFIG] config.json 未找到，使用默认配置");
        }

        // Environment overrides
        String envKey = System.getenv("ANTHROPIC_AUTH_TOKEN");
        if (envKey == null) envKey = System.getenv("AI_API_KEY");
        if (envKey == null) envKey = System.getenv("ANTHROPIC_API_KEY");
        if (StringUtils.hasText(envKey)) cfg.apiKey = envKey;

        String envUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (StringUtils.hasText(envUrl)) cfg.baseUrl = envUrl;

        String envModel = System.getenv("ANTHROPIC_MODEL");
        if (StringUtils.hasText(envModel)) cfg.model = envModel;

        return cfg;
    }

    @ConfigurationProperties(prefix = "app")
    public static class PipelineConfig {
        private String apiKey = "";
        private String baseUrl = "https://api.anthropic.com";
        private String model = "claude-sonnet-5-5";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int concurrency = 3;
        private int port = 3000;
        private String host = "localhost";
        private int maxSegments = 140;
        private int defaultSegments = 90;
        private String outputDir = "./outputs";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getMaxSegments() { return maxSegments; }
        public void setMaxSegments(int maxSegments) { this.maxSegments = maxSegments; }
        public int getDefaultSegments() { return defaultSegments; }
        public void setDefaultSegments(int defaultSegments) { this.defaultSegments = defaultSegments; }
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

        public Path getOutputPath() {
            Path p = Paths.get(outputDir);
            if (!p.isAbsolute()) {
                p = Paths.get("").toAbsolutePath().resolve(outputDir);
            }
            return p;
        }

        public Path getPromptsPath() {
            return Paths.get("").toAbsolutePath().resolve("prompts");
        }

        public Path getStateFilePath() {
            return Paths.get("").toAbsolutePath().resolve("pipeline_state.json");
        }
    }
}