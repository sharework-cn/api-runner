package com.example.apirunner.config;

/**
 * QPS控制方式枚举
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
public enum QpsControlMode {
    /**
     * 通过延迟控制QPS
     */
    DELAY,
    
    /**
     * 通过CPU消耗控制QPS
     */
    CPU
}
