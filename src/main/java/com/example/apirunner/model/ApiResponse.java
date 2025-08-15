package com.example.apirunner.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * API统一响应模型
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 响应码
     */
    private String resultCode;

    /**
     * 响应消息
     */
    private String resultMessage;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(String resultCode, String resultMessage) {
        this();
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
    }

    public ApiResponse(String resultCode, String resultMessage, T data) {
        this(resultCode, resultMessage);
        this.data = data;
    }

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("000000", "success");
    }

    /**
     * 创建成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("000000", "success", data);
    }

    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> error(String resultCode, String resultMessage) {
        return new ApiResponse<>(resultCode, resultMessage);
    }

    // Getters and Setters
    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
