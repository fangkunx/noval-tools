package com.noval.txt2img.domain.model;

/**
 * Types of parameters a model can accept.
 */
public enum ParamType {
    SELECT,   // dropdown with predefined options
    TEXT,     // free text input
    NUMBER,   // numeric input
    IMAGES    // image/file upload
}