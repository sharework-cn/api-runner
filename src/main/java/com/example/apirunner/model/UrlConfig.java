package com.example.apirunner.model;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * URL配置模型
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
public class UrlConfig {

    /**
     * URL模式（支持通配符）
     */
    private String pattern;

    /**
     * 目标QPS
     */
    private int qps;

    /**
     * 最大并发数（兼容旧版本）
     */
    private int concurrent;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 并发级别配置列表（新版本支持多个并发级别）
     */
    private List<ConcurrentLevelConfig> concurrentLevels = new ArrayList<>();

    /**
     * 编译后的正则表达式
     */
    private transient Pattern compiledPattern;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    public UrlConfig() {
    }

    public UrlConfig(String pattern, int qps, int concurrent, String description) {
        this.pattern = pattern;
        this.qps = qps;
        this.concurrent = concurrent;
        this.description = description;
        compilePattern();
    }

    public UrlConfig(String pattern, List<ConcurrentLevelConfig> concurrentLevels, String description) {
        this.pattern = pattern;
        this.concurrentLevels = concurrentLevels;
        this.description = description;
        compilePattern();
    }

    /**
     * 编译URL模式为正则表达式
     */
    public void compilePattern() {
        if (pattern != null) {
            // 将通配符转换为正则表达式
            String regex = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", "\\?");
            this.compiledPattern = Pattern.compile(regex);
        }
    }

    /**
     * 检查URL是否匹配此模式
     */
    public boolean matches(String url) {
        if (compiledPattern == null) {
            compilePattern();
        }
        return compiledPattern != null && compiledPattern.matcher(url).matches();
    }

    // Getters and Setters
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        compilePattern();
    }

    public int getQps() {
        return qps;
    }

    public void setQps(int qps) {
        this.qps = qps;
    }

    public int getConcurrent() {
        return concurrent;
    }

    public void setConcurrent(int concurrent) {
        this.concurrent = concurrent;
    }

    public List<ConcurrentLevelConfig> getConcurrentLevels() {
        return concurrentLevels;
    }

    public void setConcurrentLevels(List<ConcurrentLevelConfig> concurrentLevels) {
        this.concurrentLevels = concurrentLevels;
    }

    /**
     * 根据当前并发数查找最匹配的并发级别配置
     * 支持误差容忍度：90% - 120%
     */
    public ConcurrentLevelConfig findBestMatchingLevel(int currentConcurrent) {
        if (concurrentLevels == null || concurrentLevels.isEmpty()) {
            // 如果没有配置并发级别，使用旧的并发数配置
            return new ConcurrentLevelConfig(concurrent, qps, 5.0, description);
        }

        ConcurrentLevelConfig bestMatch = null;
        double bestScore = 0.0;

        for (ConcurrentLevelConfig level : concurrentLevels) {
            if (level.matchesLevel(currentConcurrent)) {
                double score = level.calculateMatchScore(currentConcurrent);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = level;
                }
            }
        }

        return bestMatch;
    }

    /**
     * 获取当前并发数对应的目标QPS
     */
    public int getTargetQpsForConcurrent(int currentConcurrent) {
        ConcurrentLevelConfig level = findBestMatchingLevel(currentConcurrent);
        if (level != null) {
            return level.getTargetQps();
        }
        
        // 如果没有找到匹配的级别，返回默认QPS
        return qps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    @Override
    public String toString() {
        return "UrlConfig{" +
                "pattern='" + pattern + '\'' +
                ", qps=" + qps +
                ", concurrent=" + concurrent +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
