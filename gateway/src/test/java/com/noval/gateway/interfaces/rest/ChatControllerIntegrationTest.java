package com.noval.gateway.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noval.gateway.interfaces.rest.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Chat endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_KEY = "gw-default-key-12345";

    @Test
    void chat_withoutAuth_returns401() throws Exception {
        ChatRequest request = createSimpleChatRequest();

        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_withValidApiKey_returns200() throws Exception {
        ChatRequest request = createSimpleChatRequest();

        mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("chat.completion"))
                .andExpect(jsonPath("$.choices").isArray());
    }

    @Test
    void chat_withInvalidApiKey_returns401() throws Exception {
        ChatRequest request = createSimpleChatRequest();

        mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", "Bearer invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chat_withVisionContent_parsesCorrectly() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setModel("gpt-4o");

        ChatRequest.Message message = new ChatRequest.Message();
        message.setRole("user");

        List<Object> contentParts = new ArrayList<>();

        // Text part
        java.util.Map<String, Object> textPart = new java.util.HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", "What is in this image?");
        contentParts.add(textPart);

        // Image part
        java.util.Map<String, Object> imagePart = new java.util.HashMap<>();
        imagePart.put("type", "image_url");
        java.util.Map<String, Object> imageUrl = new java.util.HashMap<>();
        imageUrl.put("url", "https://example.com/image.jpg");
        imagePart.put("image_url", imageUrl);
        contentParts.add(imagePart);

        message.setContent(contentParts);
        request.setMessages(java.util.Collections.singletonList(message));

        mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", "Bearer " + API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private ChatRequest createSimpleChatRequest() {
        ChatRequest request = new ChatRequest();
        request.setModel("gpt-4o");

        ChatRequest.Message message = new ChatRequest.Message();
        message.setRole("user");
        message.setContent("Hello, how are you?");

        request.setMessages(java.util.Collections.singletonList(message));
        return request;
    }
}
