package com.shrwk.apirunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Runner 主启动类
 * 
 * @author API Runner Team
 * @since 1.0.0
 */
@SpringBootApplication
public class ApiRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiRunnerApplication.class, args);
        System.out.println("🚀 API Runner 服务启动成功！");
        System.out.println("📊 服务地址: http://localhost:8080");
        System.out.println("📈 监控地址: http://localhost:8080/actuator");
    }
}
