package com.noval.gateway.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAI-compatible error response.
 */
@Schema(description = "错误响应 - 兼容 OpenAI 协议")
public class ErrorResponse {

    @Schema(description = "错误详情")
    private ErrorDetail error;

    public ErrorResponse(String message, String type, String code) {
        this.error = new ErrorDetail();
        this.error.message = message;
        this.error.type = type;
        this.error.code = code;
    }

    public ErrorDetail getError() {
        return error;
    }

    @Schema(description = "错误详情")
    public static class ErrorDetail {
        @Schema(description = "错误描述信息", example = "Model not found")
        private String message;

        @Schema(description = "错误类型", example = "invalid_request_error")
        private String type;

        @Schema(description = "错误代码", example = "model_not_found")
        private String code;

        public String getMessage() {
            return message;
        }

        public String getType() {
            return type;
        }

        public String getCode() {
            return code;
        }
    }
}
