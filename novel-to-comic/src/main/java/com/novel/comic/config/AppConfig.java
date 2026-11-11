package com.novel.comic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
        public int getMinSegmentChars() { return minSegmentChars; }
        public void setMinSegmentChars(int minSegmentChars) { this.minSegmentChars = minSegmentChars; }
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
        public List<String> getOutputFormats() { return outputFormats; }
        public void setOutputFormats(List<String> outputFormats) { this.outputFormats = outputFormats; }

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