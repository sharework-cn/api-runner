package com.example.apirunner.service;

import com.example.apirunner.config.ApiConfig;
import com.example.apirunner.model.UrlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 并发控制服务
 * 实现真正的并发检测和控制
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@Service
public class ConcurrentControlService {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentControlService.class);

    @Autowired
    private ApiConfig apiConfig;

    /**
     * URL对应的并发统计器
     */
    private final ConcurrentHashMap<String, ConcurrentCounter> concurrentCounters = new ConcurrentHashMap<>();

    /**
     * 全局线程池
     */
    private ThreadPoolExecutor globalThreadPool;

    /**
     * 请求队列
     */
    private final BlockingQueue<Runnable> requestQueue;

    /**
     * 活跃连接数
     */
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    /**
     * 总请求数
     */
    private final AtomicLong totalRequests = new AtomicLong(0);

    /**
     * 总响应时间
     */
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    public ConcurrentControlService() {
        // 创建有界队列，防止内存溢出
        this.requestQueue = new LinkedBlockingQueue<>(10000);
        
        // 线程池在@PostConstruct中初始化，避免构造函数中的依赖注入问题
        this.globalThreadPool = null;
    }

    @PostConstruct
    public void init() {
        // 初始化线程池
        if (globalThreadPool == null) {
            this.globalThreadPool = new ThreadPoolExecutor(
                apiConfig.getThreadPoolSize(),           // 核心线程数
                apiConfig.getThreadPoolSize() * 2,       // 最大线程数
                60L,                                    // 空闲线程存活时间
                TimeUnit.SECONDS,                       // 时间单位
                requestQueue,                           // 工作队列
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者运行
            );
        }
        
        logger.info("并发控制服务初始化完成，线程池大小: {}", apiConfig.getThreadPoolSize());
        
        // 根据配置决定是否启动监控线程
        if (apiConfig.isEnableConcurrentMonitor()) {
            startMonitoringThread();
            logger.info("并发监控已启用，输出间隔: {}秒", apiConfig.getConcurrentMonitorInterval());
        } else {
            logger.info("并发监控已禁用");
        }
    }

    @PreDestroy
    public void destroy() {
        if (globalThreadPool != null) {
            globalThreadPool.shutdown();
            try {
                if (!globalThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    globalThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                globalThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检查并发数是否超限
     */
    public boolean checkConcurrentLimit(String url, int maxConcurrent) {
        ConcurrentCounter counter = concurrentCounters.computeIfAbsent(url, k -> new ConcurrentCounter(url, maxConcurrent));
        
        // 检查当前并发数
        int currentConcurrent = counter.getCurrentConcurrent();
        if (currentConcurrent >= maxConcurrent) {
            logger.warn("URL: {} 并发数超限，当前: {}, 最大: {}", url, currentConcurrent, maxConcurrent);
            return false;
        }
        
        // 增加并发计数
        counter.incrementConcurrent();
        return true;
    }

    /**
     * 根据当前并发数查找对应的并发级别配置
     * 支持误差容忍度：90% - 120%
     */
    public int findConcurrentLevel(String url, int currentConcurrent) {
        // 获取该URL的所有并发级别配置
        // 这里需要从UrlValidationService获取配置信息
        // 暂时返回默认值，后续会完善
        return currentConcurrent;
    }

    /**
     * 根据当前并发数查找对应的目标QPS
     * 支持误差容忍度：90% - 120%
     */
    public int findTargetQpsForConcurrent(String url, int currentConcurrent) {
        // 这里需要从UrlValidationService获取URL配置
        // 然后调用UrlConfig.findBestMatchingLevel()方法
        // 暂时返回默认值，后续会完善
        return apiConfig.getDefaultQps();
    }

    /**
     * 获取指定URL的当前并发数
     */
    public int getCurrentConcurrent(String url) {
        ConcurrentCounter counter = concurrentCounters.get(url);
        return counter != null ? counter.getCurrentConcurrent() : 0;
    }

    /**
     * 释放并发计数
     */
    public void releaseConcurrent(String url) {
        ConcurrentCounter counter = concurrentCounters.get(url);
        if (counter != null) {
            counter.decrementConcurrent();
        }
    }

    /**
     * 获取并发统计信息
     */
    public ConcurrentStats getConcurrentStats(String url) {
        ConcurrentCounter counter = concurrentCounters.get(url);
        if (counter == null) {
            return new ConcurrentStats(url, 0, 0, 0);
        }
        return counter.getStats();
    }

    /**
     * 获取全局统计信息
     */
    public GlobalStats getGlobalStats() {
        int threadPoolActive = 0;
        int queueSize = 0;
        long completedTasks = 0;
        
        if (globalThreadPool != null) {
            threadPoolActive = globalThreadPool.getActiveCount();
            queueSize = globalThreadPool.getQueue().size();
            completedTasks = globalThreadPool.getCompletedTaskCount();
        }
        
        return new GlobalStats(
            activeConnections.get(),
            totalRequests.get(),
            totalResponseTime.get(),
            threadPoolActive,
            queueSize,
            completedTasks
        );
    }

    /**
     * 记录请求响应时间
     */
    public void recordResponseTime(long responseTimeMs) {
        totalResponseTime.addAndGet(responseTimeMs);
        totalRequests.incrementAndGet();
    }

    /**
     * 启动监控线程
     */
    private void startMonitoringThread() {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 根据配置的间隔输出统计信息
                    Thread.sleep(apiConfig.getConcurrentMonitorInterval() * 1000);
                    
                    GlobalStats stats = getGlobalStats();
                    logger.info("全局统计 - 活跃连接: {}, 总请求: {}, 平均响应时间: {}ms, 线程池活跃: {}, 队列长度: {}", 
                              stats.getActiveConnections(), 
                              stats.getTotalRequests(),
                              stats.getAverageResponseTime(),
                              stats.getThreadPoolActive(),
                              stats.getQueueSize());
                    
                    // 清理过期的计数器
                    cleanupExpiredCounters();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "concurrent-monitor");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 清理过期的并发计数器
     */
    private void cleanupExpiredCounters() {
        long currentTime = System.currentTimeMillis();
        concurrentCounters.entrySet().removeIf(entry -> {
            ConcurrentCounter counter = entry.getValue();
            return currentTime - counter.getLastAccessTime() > 300000; // 5分钟无访问则清理
        });
    }

    /**
     * 并发计数器内部类
     */
    private static class ConcurrentCounter {
        private final String url;
        private final int maxConcurrent;
        private final AtomicInteger currentConcurrent = new AtomicInteger(0);
        private volatile long lastAccessTime = System.currentTimeMillis();

        public ConcurrentCounter(String url, int maxConcurrent) {
            this.url = url;
            this.maxConcurrent = maxConcurrent;
        }

        public int getCurrentConcurrent() {
            return currentConcurrent.get();
        }

        public void incrementConcurrent() {
            currentConcurrent.incrementAndGet();
            lastAccessTime = System.currentTimeMillis();
        }

        public void decrementConcurrent() {
            currentConcurrent.decrementAndGet();
            lastAccessTime = System.currentTimeMillis();
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public ConcurrentStats getStats() {
            return new ConcurrentStats(url, maxConcurrent, currentConcurrent.get(), System.currentTimeMillis());
        }
    }

    /**
     * 并发统计信息
     */
    public static class ConcurrentStats {
        private final String url;
        private final int maxConcurrent;
        private final int currentConcurrent;
        private final long timestamp;

        public ConcurrentStats(String url, int maxConcurrent, int currentConcurrent, long timestamp) {
            this.url = url;
            this.maxConcurrent = maxConcurrent;
            this.currentConcurrent = currentConcurrent;
            this.timestamp = timestamp;
        }

        // Getters
        public String getUrl() { return url; }
        public int getMaxConcurrent() { return maxConcurrent; }
        public int getCurrentConcurrent() { return currentConcurrent; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 全局统计信息
     */
    public static class GlobalStats {
        private final int activeConnections;
        private final long totalRequests;
        private final long totalResponseTime;
        private final int threadPoolActive;
        private final int queueSize;
        private final long completedTasks;

        public GlobalStats(int activeConnections, long totalRequests, long totalResponseTime, 
                         int threadPoolActive, int queueSize, long completedTasks) {
            this.activeConnections = activeConnections;
            this.totalRequests = totalRequests;
            this.totalResponseTime = totalResponseTime;
            this.threadPoolActive = threadPoolActive;
            this.queueSize = queueSize;
            this.completedTasks = completedTasks;
        }

        // Getters
        public int getActiveConnections() { return activeConnections; }
        public long getTotalRequests() { return totalRequests; }
        public long getTotalResponseTime() { return totalResponseTime; }
        public int getThreadPoolActive() { return threadPoolActive; }
        public int getQueueSize() { return queueSize; }
        public long getCompletedTasks() { return completedTasks; }
        
        public long getAverageResponseTime() {
            return totalRequests > 0 ? totalResponseTime / totalRequests : 0;
        }
    }
}
