package com.novel.comic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(AppConfig.PipelineConfig.class)
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @ConfigurationProperties(prefix = "app")
    public static class PipelineConfig {
        private String apiKey = "";
        private String baseUrl = "https://api.anthropic.com";
        private String model = "claude-sonnet-5-5";
        /** AI 提供商: anthropic / deepseek */
        private String provider = "anthropic";
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private int concurrency = 3;
        private int port = 3000;
        private String host = "localhost";
        private int maxSegments = 140;
        private int defaultSegments = 90;
        private int minSegmentChars = 50;
        private String outputDir = "./outputs";
        private List<String> outputFormats = Arrays.asList("json", "txt");
        private String defaultStyle = "韩漫"; // 默认画风

        // 画风配置：key=画风名，value=提示词模板文件名
        private Map<String, String> styleTemplates = new HashMap<>();

        public PipelineConfig() {
            // 初始化默认画风模板映射
            styleTemplates.put("韩漫", "style-korean");
            styleTemplates.put("清冷都市", "style-urban");
            styleTemplates.put("柔光韩漫", "style-soft");
            styleTemplates.put("Q 版", "style-chibi");
            styleTemplates.put("软萌都市", "style-cute-urban");
            styleTemplates.put("古风", "style-ancient");
        }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
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
        public int getMinSegmentChars() { return minSegmentChars; }
        public void setMinSegmentChars(int minSegmentChars) { this.minSegmentChars = minSegmentChars; }
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
        public List<String> getOutputFormats() { return outputFormats; }
        public void setOutputFormats(List<String> outputFormats) { this.outputFormats = outputFormats; }
        public String getDefaultStyle() { return defaultStyle; }
        public void setDefaultStyle(String defaultStyle) { this.defaultStyle = defaultStyle; }
        public Map<String, String> getStyleTemplates() { return styleTemplates; }
        public void setStyleTemplates(Map<String, String> styleTemplates) { this.styleTemplates = styleTemplates; }

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