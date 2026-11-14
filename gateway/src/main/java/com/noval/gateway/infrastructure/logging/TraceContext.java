package com.noval.gateway.infrastructure.logging;

import org.slf4j.MDC;
import java.util.UUID;

/**
 * 链路追踪工具类
 * 支持 LangSmith 规范的 trace 树结构
 */
public class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String PARENT_RUN_ID = "parentRunId";
    public static final String MODEL_NAME = "modelName";
    public static final String START_TIME = "startTime";

    /**
     * 初始化链路追踪上下文
     */
    public static void init() {
        String traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
        MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 设置当前模型名称
     */
    public static void setModel(String modelName) {
        MDC.put(MODEL_NAME, modelName);
    }

    /**
     * 设置 parent run id（用于 LangSmith run 树）
     */
    public static void setParentRunId(String parentRunId) {
        if (parentRunId != null) {
            MDC.put(PARENT_RUN_ID, parentRunId);
        }
    }

    /**
     * 获取当前 traceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 获取当前 spanId (作为 run id)
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /**
     * 获取 parent run id
     */
    public static String getParentRunId() {
        return MDC.get(PARENT_RUN_ID);
    }

    /**
     * 获取当前 model
     */
    public static String getModel() {
        return MDC.get(MODEL_NAME);
    }

    /**
     * 获取开始时间
     */
    public static long getStartTime() {
        String time = MDC.get(START_TIME);
        return time != null ? Long.parseLong(time) : System.currentTimeMillis();
    }

    /**
     * 生成新的 span id
     */
    public static String newSpanId() {
        String spanId = UUID.randomUUID().toString();
        MDC.put(SPAN_ID, spanId);
        return spanId;
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        MDC.clear();
    }
}

