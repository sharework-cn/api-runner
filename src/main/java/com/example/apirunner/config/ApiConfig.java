package com.example.apirunner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API配置类
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    /**
     * 是否启用QPS控制
     */
    private boolean qpsControlEnabled = true;

    /**
     * 默认QPS（当URL未配置时使用）
     */
    private int defaultQps = 1000;

    /**
     * 并发控制线程池大小
     */
    private int threadPoolSize = 100;

    /**
     * QPS控制时间窗口（毫秒）
     */
    private int qpsWindowMs = 1000;

    /**
     * QPS误差容忍度（百分比）
     */
    private double qpsTolerance = 5.0;

    /**
     * QPS控制方式：DELAY(延迟) 或 CPU(CPU消耗)
     */
    private QpsControlMode qpsControlMode = QpsControlMode.DELAY;

    /**
     * CPU消耗时的循环次数（仅在CPU模式下使用）
     */
    private int cpuLoopCount = 10000;

    /**
     * 是否启用并发监控日志
     */
    private boolean enableConcurrentMonitor = true;

    /**
     * 并发监控日志输出间隔（秒）
     */
    private int concurrentMonitorInterval = 5;

    public boolean isQpsControlEnabled() {
        return qpsControlEnabled;
    }

    public void setQpsControlEnabled(boolean qpsControlEnabled) {
        this.qpsControlEnabled = qpsControlEnabled;
    }

    public int getDefaultQps() {
        return defaultQps;
    }

    public void setDefaultQps(int defaultQps) {
        this.defaultQps = defaultQps;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getQpsWindowMs() {
        return qpsWindowMs;
    }

    public void setQpsWindowMs(int qpsWindowMs) {
        this.qpsWindowMs = qpsWindowMs;
    }

    public double getQpsTolerance() {
        return qpsTolerance;
    }

    public void setQpsTolerance(double qpsTolerance) {
        this.qpsTolerance = qpsTolerance;
    }

    public QpsControlMode getQpsControlMode() {
        return qpsControlMode;
    }

    public void setQpsControlMode(QpsControlMode qpsControlMode) {
        this.qpsControlMode = qpsControlMode;
    }

    public int getCpuLoopCount() {
        return cpuLoopCount;
    }

    public void setCpuLoopCount(int cpuLoopCount) {
        this.cpuLoopCount = cpuLoopCount;
    }

    public boolean isEnableConcurrentMonitor() {
        return enableConcurrentMonitor;
    }

    public void setEnableConcurrentMonitor(boolean enableConcurrentMonitor) {
        this.enableConcurrentMonitor = enableConcurrentMonitor;
    }

    public int getConcurrentMonitorInterval() {
        return concurrentMonitorInterval;
    }

    public void setConcurrentMonitorInterval(int concurrentMonitorInterval) {
        this.concurrentMonitorInterval = concurrentMonitorInterval;
    }
}
