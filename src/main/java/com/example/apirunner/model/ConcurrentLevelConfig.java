package com.example.apirunner.model;

/**
 * 并发级别配置模型
 * 支持为同一URL配置多个并发级别对应的QPS
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
public class ConcurrentLevelConfig {

    /**
     * 并发级别阈值
     */
    private int concurrentLevel;

    /**
     * 对应的目标QPS
     */
    private int targetQps;

    /**
     * 误差容忍度（百分比，默认5%）
     */
    private double tolerance = 5.0;

    /**
     * 描述信息
     */
    private String description;

    public ConcurrentLevelConfig() {
    }

    public ConcurrentLevelConfig(int concurrentLevel, int targetQps) {
        this.concurrentLevel = concurrentLevel;
        this.targetQps = targetQps;
    }

    public ConcurrentLevelConfig(int concurrentLevel, int targetQps, double tolerance) {
        this.concurrentLevel = concurrentLevel;
        this.targetQps = targetQps;
        this.tolerance = tolerance;
    }

    public ConcurrentLevelConfig(int concurrentLevel, int targetQps, double tolerance, String description) {
        this.concurrentLevel = concurrentLevel;
        this.targetQps = targetQps;
        this.tolerance = tolerance;
        this.description = description;
    }

    /**
     * 检查当前并发数是否匹配此级别
     * 支持误差容忍度：90% - 120%
     */
    public boolean matchesLevel(int currentConcurrent) {
        double lowerBound = concurrentLevel * 0.9;  // 90%
        double upperBound = concurrentLevel * 1.2;  // 120%
        
        return currentConcurrent >= lowerBound && currentConcurrent <= upperBound;
    }

    /**
     * 计算与当前并发数的匹配度（0.0 - 1.0）
     * 1.0表示完全匹配，0.0表示完全不匹配
     */
    public double calculateMatchScore(int currentConcurrent) {
        if (!matchesLevel(currentConcurrent)) {
            return 0.0;
        }
        
        // 计算匹配度：越接近目标并发级别，匹配度越高
        double distance = Math.abs(currentConcurrent - concurrentLevel);
        double maxDistance = concurrentLevel * 0.3; // 最大允许距离（30%）
        
        if (distance >= maxDistance) {
            return 0.0;
        }
        
        return 1.0 - (distance / maxDistance);
    }

    // Getters and Setters
    public int getConcurrentLevel() {
        return concurrentLevel;
    }

    public void setConcurrentLevel(int concurrentLevel) {
        this.concurrentLevel = concurrentLevel;
    }

    public int getTargetQps() {
        return targetQps;
    }

    public void setTargetQps(int targetQps) {
        this.targetQps = targetQps;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ConcurrentLevelConfig{" +
                "concurrentLevel=" + concurrentLevel +
                ", targetQps=" + targetQps +
                ", tolerance=" + tolerance +
                ", description='" + description + '\'' +
                '}';
    }
}
