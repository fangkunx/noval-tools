package com.noval.gateway.interfaces.rest;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenAI-compatible chat completion endpoint.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Chat", description = "聊天和视觉能力 - OpenAI 兼容协议")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
            summary = "聊天补全",
            description = "支持纯文本对话和视觉理解（当 messages 中包含 image_url 时自动使用视觉模型）"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求错误", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/chat/completions")
    public Object chat(
            @Parameter(description = "聊天请求", required = true)
            @RequestBody ChatRequest request,
            @Parameter(hidden = true)
            @RequestHeader(value = "Accept", required = false) String accept) {
        log.debug("Chat request: model={}, stream={}", request.getModel(), request.isStream());

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
}
