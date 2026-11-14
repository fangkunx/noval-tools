package com.noval.txt2img.interfaces.rest.dto;

/**
 * Typed error response — replaces Map<String, String> for errors.
 */
public class ErrorResponse {

    private final String state;
    private final String error;
    private final long timestamp;

    public ErrorResponse(String state, String error) {
        this.state = state;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    public String getState() { return state; }
    public String getError() { return error; }
    public long getTimestamp() { return timestamp; }
}