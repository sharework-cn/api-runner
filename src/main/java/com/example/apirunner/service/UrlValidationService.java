package com.example.apirunner.service;

import com.example.apirunner.config.ApiConfig;
import com.example.apirunner.model.UrlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * URL验证服务
 * 负责验证请求URL是否在配置的白名单中
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@Service
public class UrlValidationService {

    private static final Logger logger = LoggerFactory.getLogger(UrlValidationService.class);

    @Autowired
    private ApiConfig apiConfig;

    /**
     * URL配置列表
     */
    private final List<UrlConfig> urlConfigs = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 初始化默认配置
        initializeDefaultConfigs();
        logger.info("URL验证服务初始化完成，共配置 {} 个URL模式", urlConfigs.size());
    }

    /**
     * 初始化默认配置
     */
    private void initializeDefaultConfigs() {
        // 用户相关接口
        urlConfigs.add(new UrlConfig("/api/user/*", 500, 50, "用户相关接口"));
        
        // 订单相关接口
        urlConfigs.add(new UrlConfig("/api/order/*", 200, 30, "订单相关接口"));
        
        // 产品相关接口
        urlConfigs.add(new UrlConfig("/api/product/*", 1000, 100, "产品相关接口"));
        
        // 系统相关接口
        urlConfigs.add(new UrlConfig("/api/system/*", 300, 20, "系统相关接口"));
        
        // 健康检查接口
        urlConfigs.add(new UrlConfig("/actuator/*", 100, 10, "监控接口"));
    }

    /**
     * 验证URL是否在允许列表中
     */
    public boolean isUrlAllowed(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 移除查询参数
        String cleanUrl = url.split("\\?")[0];
        
        // 检查是否匹配任何配置的模式
        Optional<UrlConfig> matchedConfig = urlConfigs.stream()
                .filter(UrlConfig::isEnabled)
                .filter(config -> config.matches(cleanUrl))
                .findFirst();

        if (matchedConfig.isPresent()) {
            logger.debug("URL '{}' 匹配模式 '{}'", cleanUrl, matchedConfig.get().getPattern());
            return true;
        }

        logger.debug("URL '{}' 不在允许列表中", cleanUrl);
        return false;
    }

    /**
     * 获取URL对应的配置
     */
    public Optional<UrlConfig> getUrlConfig(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }

        String cleanUrl = url.split("\\?")[0];
        
        return urlConfigs.stream()
                .filter(UrlConfig::isEnabled)
                .filter(config -> config.matches(cleanUrl))
                .findFirst();
    }

    /**
     * 获取所有URL配置
     */
    public List<UrlConfig> getAllUrlConfigs() {
        return new ArrayList<>(urlConfigs);
    }

    /**
     * 添加新的URL配置
     */
    public void addUrlConfig(UrlConfig urlConfig) {
        if (urlConfig != null && urlConfig.getPattern() != null) {
            urlConfigs.add(urlConfig);
            logger.info("添加新的URL配置: {}", urlConfig.getPattern());
        }
    }

    /**
     * 移除URL配置
     */
    public boolean removeUrlConfig(String pattern) {
        return urlConfigs.removeIf(config -> pattern.equals(config.getPattern()));
    }

    /**
     * 更新URL配置
     */
    public boolean updateUrlConfig(String pattern, int newQps, int newConcurrent) {
        for (UrlConfig config : urlConfigs) {
            if (pattern.equals(config.getPattern())) {
                config.setQps(newQps);
                config.setConcurrent(newConcurrent);
                logger.info("更新URL配置: {} -> QPS: {}, 并发: {}", pattern, newQps, newConcurrent);
                return true;
            }
        }
        return false;
    }

    /**
     * 启用/禁用URL配置
     */
    public boolean setUrlConfigEnabled(String pattern, boolean enabled) {
        for (UrlConfig config : urlConfigs) {
            if (pattern.equals(config.getPattern())) {
                config.setEnabled(enabled);
                logger.info("{} URL配置: {}", enabled ? "启用" : "禁用", pattern);
                return true;
            }
        }
        return false;
    }
}
