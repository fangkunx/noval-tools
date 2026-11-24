package com.noval.gateway.interfaces.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noval.gateway.application.ChatService;
import com.noval.gateway.domain.model.ChatCommand;
import com.noval.gateway.domain.model.ChatResult;
import com.noval.gateway.domain.spi.GatewayProvider;
import com.noval.gateway.interfaces.rest.dto.ChatRequest;
import com.noval.gateway.interfaces.rest.dto.ChatResponse;
import com.noval.gateway.interfaces.rest.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenAI-compatible chat completion endpoint.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Chat", description = "聊天和视觉能力 - OpenAI 兼容协议\n\n" +
        "**支持的 Chat 模型:**\n" +
        "- **OpenAI**: gpt-4o, gpt-4o-mini, gpt-4-turbo\n" +
        "- **Claude**: claude-opus-5-5, claude-sonnet-5-5, claude-haiku-5-5\n" +
        "- **Google Gemini**: gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash\n" +
        "- **DeepSeek**: deepseek-chat\n" +
        "- **Grsai (代理)**: gpt-4o, gpt-4o-mini, claude-sonnet-5-5, gemini-2.0-flash\n\n" +
        "**Grsai API 文档:** https://qmy27nhsd9.apifox.cn/")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final Path PROMPT_LOG_DIR = Paths.get("prompt-logs");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HH_mm_ss");

    private final ObjectMapper mapper;
    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(ChatService chatService, ObjectMapper mapper) {
        this.chatService = chatService;
        this.mapper = mapper;
    }

    @Operation(
            summary = "聊天补全",
            description = "支持纯文本对话和视觉理解（当 messages 中包含 image_url 时自动使用视觉模型）\n\n" +
                    "**可用的 Chat 模型:**\n" +
                    "- gpt-4o, gpt-4o-mini, gpt-4-turbo (OpenAI)\n" +
                    "- claude-opus-5-5, claude-sonnet-5-5, claude-haiku-5-5 (Anthropic)\n" +
                    "- gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash (Google)\n" +
                    "- deepseek-chat (DeepSeek)\n\n" +
                    "通过 Grsai 代理也可访问这些模型。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "纯文本对话",
                                            summary = "简单的文本聊天",
                                            value = "{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"system\",\"content\":\"你是一个有帮助的助手\"},{\"role\":\"user\",\"content\":\"你好！\"}],\"temperature\":0.7}"
                                    ),
                                    @ExampleObject(
                                            name = "视觉理解",
                                            summary = "带图片的多模态对话",
                                            value = "{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"描述这张图片\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\"https://example.com/image.jpg\"}}]}]}"
                                    ),
                                    @ExampleObject(
                                            name = "流式响应",
                                            summary = "启用流式输出",
                                            value = "{\"model\":\"claude-sonnet-5-5\",\"messages\":[{\"role\":\"user\",\"content\":\"写一首诗\"}],\"stream\":true}"
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求错误", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/chat/completions")
    public Object chat(
            HttpServletRequest httpServletRequest,
            @Parameter(description = "聊天请求", required = true)
            @RequestBody ChatRequest request,
            @Parameter(hidden = true)
            @RequestHeader(value = "Accept", required = false) String accept) throws JsonProcessingException {

        // 打印完整的 HTTP 请求报文
        logFullHttpRequest(httpServletRequest, request);

        log.debug("Chat request: model={}, stream={}", request.getModel(), request.isStream());

        saveMessagesToMarkdown(request.getMessages());

        // Convert DTO to domain command
        ChatCommand command = convertToCommand(request);

        // Streaming response
        if (request.isStream()) {
            return handleStreaming(command, request.getModel());
        }

        // Non-streaming response
        ChatResult result = chatService.chat(command);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.getError(), "chat_error", null));
        }

        ChatResponse response = ChatResponse.success(
                result.getId(),
                request.getModel(),
                result.getContent(),
                result.getFinishReason(),
                ChatResponse.Usage.from(result.getUsage())
        );

        return ResponseEntity.ok(response);
    }

    // --- Private helpers ---

    private SseEmitter handleStreaming(ChatCommand command, String requestedModel) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout

        executor.submit(() -> {
            try {
                chatService.chatStream(
                        command,
                        chunk -> {
                            try {
                                String data = formatSseData(chunk, requestedModel);
                                emitter.send(SseEmitter.event().data(data));
                            } catch (Exception e) {
                                log.error("Error sending SSE chunk", e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().data(
                                        "data: {\"error\": \"" + error.getMessage() + "\"}\n\n"));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().data("data: [DONE]\n\n"));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private ChatCommand convertToCommand(ChatRequest dto) {
        ChatCommand.Builder builder = ChatCommand.builder()
                .model(dto.getModel())
                .stream(dto.isStream())
                .temperature(dto.getTemperature())
                .maxTokens(dto.getMax_tokens());

        List<ChatCommand.Message> messages = new ArrayList<>();
        for (ChatRequest.Message dtoMsg : dto.getMessages()) {
            List<ChatCommand.ContentPart> parts = new ArrayList<>();

            if (dtoMsg.getContent() instanceof String) {
                parts.add(ChatCommand.ContentPart.text((String) dtoMsg.getContent()));
            } else if (dtoMsg.getContent() instanceof List) {
                for (Object partObj : (List<?>) dtoMsg.getContent()) {
                    if (partObj instanceof java.util.Map) {
                        java.util.Map<?, ?> partMap = (java.util.Map<?, ?>) partObj;
                        String type = (String) partMap.get("type");
                        if ("text".equals(type)) {
                            parts.add(ChatCommand.ContentPart.text((String) partMap.get("text")));
                        } else if ("image_url".equals(type)) {
                            java.util.Map<?, ?> imageUrl = (java.util.Map<?, ?>) partMap.get("image_url");
                            if (imageUrl != null) {
                                parts.add(ChatCommand.ContentPart.imageUrl((String) imageUrl.get("url")));
                            }
                        }
                    }
                }
            }

            if (!parts.isEmpty()) {
                messages.add(new ChatCommand.Message(dtoMsg.getRole(), parts));
            }
        }

        return builder.messages(messages).build();
    }

    private String formatSseData(GatewayProvider.ChatStreamChunk chunk, String model) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(chunk.getId() != null ? chunk.getId() : "").append("\",");
        sb.append("\"object\":\"chat.completion.chunk\",");
        sb.append("\"created\":").append(System.currentTimeMillis() / 1000).append(",");
        sb.append("\"model\":\"").append(model != null ? model : "").append("\",");
        sb.append("\"choices\":[{");
        sb.append("\"index\":0,");
        sb.append("\"delta\":{");
        if (chunk.getContent() != null) {
            sb.append("\"content\":\"").append(escapeJson(chunk.getContent())).append("\"");
        }
        sb.append("},");
        if (chunk.getFinishReason() != null) {
            sb.append("\"finish_reason\":\"").append(chunk.getFinishReason()).append("\"");
        } else {
            sb.append("\"finish_reason\":null");
        }
        sb.append("}]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 将请求消息保存为 Markdown 文件
     * 文件名格式：提示词-yyyyMMdd_HH_mm_ss.md
     */
    private void saveMessagesToMarkdown(List<ChatRequest.Message> messages) {
        try {
            if (!Files.exists(PROMPT_LOG_DIR)) {
                Files.createDirectories(PROMPT_LOG_DIR);
            }

            String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
            Path filePath = PROMPT_LOG_DIR.resolve("提示词-" + timestamp + ".md");

            StringBuilder sb = new StringBuilder();
            sb.append("# 请求消息记录\n\n");
            sb.append("**时间:** ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            sb.append("---\n\n");

            for (ChatRequest.Message msg : messages) {
                sb.append("## ").append(msg.getRole()).append("\n\n");

                Object content = msg.getContent();
                if (content instanceof String) {
                    sb.append(content).append("\n\n");
                } else if (content instanceof List) {
                    for (Object partObj : (List<?>) content) {
                        if (partObj instanceof java.util.Map) {
                            java.util.Map<?, ?> partMap = (java.util.Map<?, ?>) partObj;
                            String type = (String) partMap.get("type");
                            if ("text".equals(type)) {
                                sb.append(partMap.get("text")).append("\n\n");
                            } else if ("image_url".equals(type)) {
                                java.util.Map<?, ?> imageUrl = (java.util.Map<?, ?>) partMap.get("image_url");
                                if (imageUrl != null) {
                                    sb.append("![image](").append(imageUrl.get("url")).append(")\n\n");
                                }
                            }
                        }
                    }
                }
                sb.append("---\n\n");
            }

            Files.write(filePath, sb.toString().getBytes(StandardCharsets.UTF_8));
            log.info("消息已保存到: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("保存消息到 Markdown 文件失败", e);
        }
    }

    /**
     * 打印完整的 HTTP 请求报文信息
     */
    private void logFullHttpRequest(HttpServletRequest request, ChatRequest chatRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== HTTP Request Dump ==========\n");

        // 1. 请求行信息
        sb.append("【请求行】\n");
        sb.append("  Method: ").append(request.getMethod()).append("\n");
        sb.append("  RequestURI: ").append(request.getRequestURI()).append("\n");
        sb.append("  QueryString: ").append(request.getQueryString()).append("\n");
        sb.append("  Protocol: ").append(request.getProtocol()).append("\n");
        sb.append("  Scheme: ").append(request.getScheme()).append("\n");
        sb.append("  ServerName: ").append(request.getServerName()).append("\n");
        sb.append("  ServerPort: ").append(request.getServerPort()).append("\n");
        sb.append("  ContextPath: ").append(request.getContextPath()).append("\n");
        sb.append("  ServletPath: ").append(request.getServletPath()).append("\n");
        sb.append("  PathInfo: ").append(request.getPathInfo()).append("\n");
        sb.append("  RemoteAddr: ").append(request.getRemoteAddr()).append("\n");
        sb.append("  RemoteHost: ").append(request.getRemoteHost()).append("\n");
        sb.append("  RemotePort: ").append(request.getRemotePort()).append("\n");
        sb.append("  LocalAddr: ").append(request.getLocalAddr()).append("\n");
        sb.append("  LocalName: ").append(request.getLocalName()).append("\n");
        sb.append("  LocalPort: ").append(request.getLocalPort()).append("\n");
        sb.append("  CharacterEncoding: ").append(request.getCharacterEncoding()).append("\n");
        sb.append("  ContentLength: ").append(request.getContentLength()).append("\n");
        sb.append("  ContentType: ").append(request.getContentType()).append("\n");

        // 2. 所有请求头
        sb.append("\n【请求头】\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            List<String> headerList = Collections.list(headerNames);
            Collections.sort(headerList);
            for (String headerName : headerList) {
                String headerValue = request.getHeader(headerName);
                sb.append("  ").append(headerName).append(": ").append(headerValue).append("\n");
            }
        } else {
            sb.append("  (无请求头)\n");
        }

        // 3. 查询参数
        sb.append("\n【查询参数】\n");
        Enumeration<String> paramNames = request.getParameterNames();
        if (paramNames != null) {
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                sb.append("  ").append(paramName).append(": ");
                if (paramValues != null) {
                    sb.append(String.join(", ", paramValues));
                }
                sb.append("\n");
            }
        } else {
            sb.append("  (无查询参数)\n");
        }

        // 4. 请求体（通过已解析的 ChatRequest 对象打印）
        sb.append("\n【请求体 (ChatRequest)】\n");
        if (chatRequest != null) {
            try {
                sb.append("  ").append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chatRequest)).append("\n");
            } catch (JsonProcessingException e) {
                sb.append("  序列化失败: ").append(e.getMessage()).append("\n");
            }
        } else {
            sb.append("  (空请求体)\n");
        }

        sb.append("=========================================\n");
        log.info(sb.toString());
    }
}
