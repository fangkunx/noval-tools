package com.noval.gateway.interfaces.rest.dto;

import java.util.List;

/**
 * OpenAI-compatible error response.
 */
public class ErrorResponse {

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

    public static class ErrorDetail {
        private String message;
        private String type;
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
