package com.example.apirunner.controller;

import com.example.apirunner.config.ApiConfig;
import com.example.apirunner.model.ApiResponse;
import com.example.apirunner.model.UrlConfig;
import com.example.apirunner.service.QpsControlService;
import com.example.apirunner.service.UrlValidationService;
import com.example.apirunner.service.ConcurrentControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API控制器
 * 处理所有HTTP请求，实现QPS控制和URL白名单验证
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@RestController
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private UrlValidationService urlValidationService;

    @Autowired
    private QpsControlService qpsControlService;

    @Autowired
    private ConcurrentControlService concurrentControlService;

    @Autowired
    private ApiConfig apiConfig;

    /**
     * 处理所有HTTP请求
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
                                           RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, 
                                           RequestMethod.OPTIONS})
    public ResponseEntity<ApiResponse<Object>> handleAllRequests(HttpServletRequest request) {
        String method = request.getMethod();
        return processRequest(request, method);
    }

    /**
     * 统一的请求处理方法
     */
    private ResponseEntity<ApiResponse<Object>> processRequest(HttpServletRequest request, String method) {
        String requestUri = request.getRequestURI();
        String fullUrl = request.getRequestURL().toString();
        long startTime = System.currentTimeMillis();
        
        logger.info("收到 {} 请求: {}", method, requestUri);

        // 验证URL是否在允许列表中
        if (!urlValidationService.isUrlAllowed(requestUri)) {
            logger.warn("URL不在允许列表中: {}", requestUri);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("404001", "URL not found: " + requestUri));
        }

        // 获取URL配置
        Optional<UrlConfig> urlConfig = urlValidationService.getUrlConfig(requestUri);
        
        // 获取当前并发数
        int currentConcurrent = concurrentControlService.getCurrentConcurrent(requestUri);
        
        // 根据当前并发数查找对应的目标QPS（支持误差容忍度）
        int targetQps = urlConfig.map(config -> config.getTargetQpsForConcurrent(currentConcurrent))
                                .orElse(apiConfig.getDefaultQps());
        
        int maxConcurrent = urlConfig.map(UrlConfig::getConcurrent).orElse(apiConfig.getThreadPoolSize());

        // 检查并发数限制
        if (!concurrentControlService.checkConcurrentLimit(requestUri, maxConcurrent)) {
            logger.warn("并发数超限: {}", requestUri);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("429001", "Too many concurrent requests"));
        }

        try {
            // 应用QPS控制
            try {
                qpsControlService.controlQps(requestUri, targetQps);
            } catch (Exception e) {
                logger.error("QPS控制失败: {}", e.getMessage(), e);
                // 即使QPS控制失败，也继续处理请求
            }

            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("method", method);
            responseData.put("url", requestUri);
            responseData.put("timestamp", System.currentTimeMillis());
            responseData.put("targetQps", targetQps);
            
            if (urlConfig.isPresent()) {
                responseData.put("pattern", urlConfig.get().getPattern());
                responseData.put("concurrent", urlConfig.get().getConcurrent());
                responseData.put("description", urlConfig.get().getDescription());
            }

            logger.debug("请求处理完成: {} -> QPS: {}", requestUri, targetQps);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } finally {
            // 释放并发计数
            concurrentControlService.releaseConcurrent(requestUri);
            
            // 记录响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            concurrentControlService.recordResponseTime(responseTime);
        }
    }

    /**
     * 获取QPS统计信息
     */
    @GetMapping("/api/stats/qps")
    public ResponseEntity<ApiResponse<Object>> getQpsStats(@RequestParam(required = false) String url) {
        if (url != null && !url.trim().isEmpty()) {
            QpsControlService.QpsStats stats = qpsControlService.getQpsStats(url);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } else {
            // 返回所有URL的统计信息
            Map<String, Object> allStats = new HashMap<>();
            allStats.put("message", "请指定URL参数来获取特定URL的QPS统计信息");
            allStats.put("example", "/api/stats/qps?url=/api/user/123");
            return ResponseEntity.ok(ApiResponse.success(allStats));
        }
    }

    /**
     * 获取所有URL配置
     */
    @GetMapping("/api/config/urls")
    public ResponseEntity<ApiResponse<Object>> getAllUrlConfigs() {
        return ResponseEntity.ok(ApiResponse.success(urlValidationService.getAllUrlConfigs()));
    }

    /**
     * 获取并发统计信息
     */
    @GetMapping("/api/stats/concurrent")
    public ResponseEntity<ApiResponse<Object>> getConcurrentStats(@RequestParam(required = false) String url) {
        if (url != null && !url.trim().isEmpty()) {
            ConcurrentControlService.ConcurrentStats stats = concurrentControlService.getConcurrentStats(url);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } else {
            // 返回全局统计信息
            ConcurrentControlService.GlobalStats stats = concurrentControlService.getGlobalStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "API Runner");
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("qpsControlEnabled", apiConfig.isQpsControlEnabled());
        healthInfo.put("defaultQps", apiConfig.getDefaultQps());
        healthInfo.put("qpsControlMode", apiConfig.getQpsControlMode());
        healthInfo.put("cpuLoopCount", apiConfig.getCpuLoopCount());
        
        return ResponseEntity.ok(ApiResponse.success(healthInfo));
    }
}
