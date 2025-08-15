package com.example.apirunner.service;

import com.example.apirunner.config.ApiConfig;
import com.example.apirunner.model.UrlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * QPS控制服务
 * 实现精确的QPS控制，误差控制在5%以内
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@Service
public class QpsControlService {

    private static final Logger logger = LoggerFactory.getLogger(QpsControlService.class);

    @Autowired
    private ApiConfig apiConfig;

    /**
     * URL对应的QPS统计器
     */
    private final ConcurrentHashMap<String, QpsCounter> qpsCounters = new ConcurrentHashMap<>();

    /**
     * 全局锁，用于同步QPS控制
     */
    private final ReentrantLock globalLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        logger.info("QPS控制服务初始化完成，默认QPS: {}, 误差容忍度: {}%, 控制方式: {}", 
                   apiConfig.getDefaultQps(), apiConfig.getQpsTolerance(), apiConfig.getQpsControlMode());
    }

    /**
     * 控制QPS，确保不超过目标值
     */
    public void controlQps(String url, int targetQps) {
        if (!apiConfig.isQpsControlEnabled()) {
            return;
        }

        QpsCounter counter = qpsCounters.computeIfAbsent(url, k -> new QpsCounter(url, targetQps));
        
        // 检查是否需要控制QPS
        if (counter.shouldControl()) {
            if (apiConfig.getQpsControlMode() == com.example.apirunner.config.QpsControlMode.DELAY) {
                // 延迟方式控制QPS
                long delayMs = calculateDelay(counter, targetQps);
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("QPS控制延迟被中断: {}", e.getMessage());
                    }
                }
            } else {
                // CPU消耗方式控制QPS
                long cpuTimeMs = calculateCpuTime(counter, targetQps);
                if (cpuTimeMs > 0) {
                    consumeCpu(cpuTimeMs);
                }
            }
        }
        
        // 记录请求
        counter.recordRequest();
    }

    /**
     * 计算需要延迟的时间
     */
    private long calculateDelay(QpsCounter counter, int targetQps) {
        double currentQps = counter.getCurrentQps();
        double targetQpsDouble = targetQps;
        
        // 如果当前QPS在容忍范围内，不需要延迟
        double tolerance = apiConfig.getQpsTolerance() / 100.0;
        if (currentQps <= targetQpsDouble * (1 + tolerance)) {
            return 0;
        }

        // 计算需要的延迟时间
        double delaySeconds = (currentQps - targetQpsDouble) / (targetQpsDouble * targetQpsDouble);
        long delayMs = Math.round(delaySeconds * 1000);
        
        // 限制最大延迟时间，避免响应过慢
        delayMs = Math.min(delayMs, 1000);
        
        logger.debug("URL: {}, 当前QPS: {:.2f}, 目标QPS: {}, 延迟: {}ms", 
                    counter.getUrl(), currentQps, targetQps, delayMs);
        
        return delayMs;
    }

    /**
     * 计算需要消耗的CPU时间
     */
    private long calculateCpuTime(QpsCounter counter, int targetQps) {
        double currentQps = counter.getCurrentQps();
        double targetQpsDouble = targetQps;
        
        // 如果当前QPS在容忍范围内，不需要消耗CPU
        double tolerance = apiConfig.getQpsTolerance() / 100.0;
        if (currentQps <= targetQpsDouble * (1 + tolerance)) {
            return 0;
        }

        // 计算需要消耗的CPU时间（毫秒）
        double cpuTimeSeconds = (currentQps - targetQpsDouble) / (targetQpsDouble * targetQpsDouble);
        long cpuTimeMs = Math.round(cpuTimeSeconds * 1000);
        
        // 限制最大CPU消耗时间，避免响应过慢
        cpuTimeMs = Math.min(cpuTimeMs, 500);
        
        logger.debug("URL: {}, 当前QPS: {:.2f}, 目标QPS: {}, CPU消耗时间: {}ms", 
                    counter.getUrl(), currentQps, targetQps, cpuTimeMs);
        
        return cpuTimeMs;
    }

    /**
     * 消耗CPU时间
     */
    private void consumeCpu(long cpuTimeMs) {
        if (cpuTimeMs <= 0) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        long targetEndTime = startTime + cpuTimeMs;
        
        // 通过循环消耗CPU时间
        int loopCount = apiConfig.getCpuLoopCount();
        while (System.currentTimeMillis() < targetEndTime) {
            // 执行一些无意义的计算来消耗CPU
            for (int i = 0; i < loopCount; i++) {
                Math.sqrt(i * i + 1);
            }
        }
        
        long actualTime = System.currentTimeMillis() - startTime;
        logger.debug("CPU消耗完成，目标时间: {}ms, 实际时间: {}ms", cpuTimeMs, actualTime);
    }

    /**
     * 获取URL的QPS统计信息
     */
    public QpsStats getQpsStats(String url) {
        QpsCounter counter = qpsCounters.get(url);
        if (counter == null) {
            return new QpsStats(url, 0, 0, 0);
        }
        return counter.getStats();
    }

    /**
     * 清理过期的QPS统计器
     */
    public void cleanupExpiredCounters() {
        long currentTime = System.currentTimeMillis();
        qpsCounters.entrySet().removeIf(entry -> {
            QpsCounter counter = entry.getValue();
            return currentTime - counter.getLastAccessTime() > 300000; // 5分钟无访问则清理
        });
    }

    /**
     * QPS计数器内部类
     */
    private static class QpsCounter {
        private final String url;
        private final int targetQps;
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
        private volatile long lastAccessTime = System.currentTimeMillis();
        
        private static final long WINDOW_MS = 1000; // 1秒窗口

        public QpsCounter(int targetQps) {
            this.url = "unknown";
            this.targetQps = targetQps;
        }
        
        public QpsCounter(String url, int targetQps) {
            this.url = url;
            this.targetQps = targetQps;
        }

        public boolean shouldControl() {
            long currentTime = System.currentTimeMillis();
            long lastReset = lastResetTime.get();
            
            // 如果时间窗口已过，重置计数器
            if (currentTime - lastReset >= WINDOW_MS) {
                if (lastResetTime.compareAndSet(lastReset, currentTime)) {
                    requestCount.set(0);
                }
            }
            
            lastAccessTime = currentTime;
            return true;
        }

        public void recordRequest() {
            requestCount.incrementAndGet();
        }

        public double getCurrentQps() {
            long currentTime = System.currentTimeMillis();
            long lastReset = lastResetTime.get();
            long elapsed = currentTime - lastReset;
            
            if (elapsed < WINDOW_MS) {
                elapsed = WINDOW_MS;
            }
            
            return (double) requestCount.get() * 1000 / elapsed;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public String getUrl() {
            return url;
        }

        public QpsStats getStats() {
            return new QpsStats(url, targetQps, requestCount.get(), getCurrentQps());
        }
    }

    /**
     * QPS统计信息
     */
    public static class QpsStats {
        private final String url;
        private final int targetQps;
        private final long totalRequests;
        private final double currentQps;

        public QpsStats(String url, int targetQps, long totalRequests, double currentQps) {
            this.url = url;
            this.targetQps = targetQps;
            this.totalRequests = totalRequests;
            this.currentQps = currentQps;
        }

        // Getters
        public String getUrl() { return url; }
        public int getTargetQps() { return targetQps; }
        public long getTotalRequests() { return totalRequests; }
        public double getCurrentQps() { return currentQps; }
    }
}
