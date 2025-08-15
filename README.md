# API Runner - 接口压测服务

## 项目简介

API Runner 是一个基于 Spring Boot 的接口压测服务，主要用于模拟接口响应，支持配置化的QPS控制和并发限制。

## 主要功能

1. **接口模拟**: 接受任意接口请求，返回统一的成功响应
2. **URL白名单**: 通过配置文件指定可接受的URL，其他URL返回404
3. **智能QPS限流**: 根据检测到的并发级别，动态查找并实施对应的目标QPS限流
4. **并发级别检测**: 实时检测当前并发级别，为QPS限流提供依据
5. **双模式限流**: 
   - **延迟模式**: 通过Thread.sleep()控制QPS（默认，推荐）
   - **CPU模式**: 通过CPU密集型计算控制QPS（高精度）
6. **精确控制**: QPS误差控制在5%以内

## 技术架构

- **框架**: Spring Boot 2.7.18
- **Java版本**: Java 8
- **构建工具**: Maven
- **监控**: Spring Boot Actuator

## 项目结构

```
api-runner/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/apirunner/
│   │   │       ├── ApiRunnerApplication.java
│   │   │       ├── config/
│   │   │       │   ├── ApiConfig.java
│   │   │       │   └── QpsConfig.java
│   │   │       ├── controller/
│   │   │       │   └── ApiController.java
│   │   │       ├── service/
│   │   │       │   ├── QpsControlService.java
│   │   │       │   └── UrlValidationService.java
│   │   │       └── model/
│   │   │           ├── ApiResponse.java
│   │   │           └── UrlConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── urls-config.yml
│   └── test/
│       └── java/
│           └── com/example/apirunner/
├── pom.xml
└── README.md
```

## 配置说明

### 1. 应用配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: api-runner

# 自定义配置
api:
  # 是否启用QPS控制
  qps-control-enabled: true
  # 默认QPS（当URL未配置时使用）
  default-qps: 1000
  # 并发控制线程池大小
  thread-pool-size: 100
  # QPS控制方式：DELAY(延迟) 或 CPU(CPU消耗)
  qps-control-mode: DELAY
  # CPU消耗时的循环次数（仅在CPU模式下使用）
  cpu-loop-count: 10000
```

### 2. URL配置 (urls-config.yml)

#### 多并发级别配置（推荐）
```yaml
urls:
  - pattern: "/api/user/*"
    concurrentLevels:
      - concurrentLevel: 50
        targetQps: 500
        tolerance: 5.0
        description: "低并发级别：45-60并发，QPS限制500"
      - concurrentLevel: 100
        targetQps: 300
        tolerance: 5.0
        description: "中并发级别：90-120并发，QPS限制300"
      - concurrentLevel: 200
        targetQps: 150
        tolerance: 5.0
        description: "高并发级别：180-240并发，QPS限制150"
    description: "用户相关接口（智能多级别QPS控制）"
```

#### 基础配置（兼容旧版本）
```yaml
urls:
  - pattern: "/api/legacy/*"
    qps: 100
    concurrent: 50
    description: "旧版本兼容接口"
```

**配置说明：**
- `pattern`: URL模式，支持通配符 `*` 和 `?`
- `concurrentLevels`: 并发级别配置列表（推荐配置）
  - `concurrentLevel`: 并发级别阈值
  - `targetQps`: 对应的目标QPS
  - `tolerance`: 误差容忍度（百分比）
  - `description`: 级别描述
- `qps`: 当检测到对应并发级别时的目标QPS（兼容旧版本）
- `concurrent`: 并发级别阈值，用于QPS查找（兼容旧版本）
- `description`: 接口描述信息

## 使用方法

### 1. 启动服务

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/api-runner-1.0.0.jar
```

### 2. 接口调用

服务启动后，可以通过以下方式调用：

```bash
# 配置的URL - 返回成功响应
curl http://localhost:8080/api/user/123
# 响应: {"resultCode": "000000", "resultMessage": "success"}

# 未配置的URL - 返回404
curl http://localhost:8080/api/unknown
# 响应: 404 Not Found
```

### 3. 监控接口

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info
```

## 智能QPS限流原理

系统通过以下机制实现智能的QPS限流：

1. **并发级别检测**: 实时检测当前并发请求数，识别并发级别
2. **动态QPS查找**: 根据检测到的并发级别，从配置中查找对应的目标QPS
3. **智能限流调节**: 当实际QPS超过目标值时，自动调节响应速度
4. **双模式控制**: 
   - **延迟模式**: 通过Thread.sleep()增加延迟时间
   - **CPU模式**: 通过CPU密集型计算消耗时间
5. **误差控制**: 确保QPS误差控制在5%以内

### 设计理念

**并发控制由调用方负责，我们的服务专注于智能QPS限流：**

#### 核心思想
我们的服务**不控制并发数**，而是作为一个**智能的QPS限流器**，能够：
1. **检测当前并发级别**：实时统计当前活跃请求数
2. **动态查找目标QPS**：根据并发级别从配置中查找对应的目标QPS
3. **实施精确限流**：通过延迟或CPU消耗确保实际QPS不超过目标值

#### 工作流程示例
```
调用方发起1000个并发请求
    ↓
我们的服务检测到当前并发级别：1000
    ↓
查找配置：1000并发 → 目标QPS 500
    ↓
实施QPS限流：确保实际QPS ≤ 500
    ↓
返回成功响应，但响应时间被适当延长
```

#### 配置示例
```yaml
urls:
  - pattern: "/api/user/*"
    concurrentLevels:
      - concurrentLevel: 50
        targetQps: 500      # 当检测到45-60并发时，限制QPS为500
        tolerance: 5.0      # 误差容忍度：90% - 120%
        description: "低并发级别"
      - concurrentLevel: 100
        targetQps: 300      # 当检测到90-120并发时，限制QPS为300
        tolerance: 5.0      # 误差容忍度：90% - 120%
        description: "中并发级别"
      - concurrentLevel: 200
        targetQps: 150      # 当检测到180-240并发时，限制QPS为150
        tolerance: 5.0      # 误差容忍度：90% - 120%
        description: "高并发级别"
```

#### 误差容忍度说明
- **90% - 120%范围**：当实际并发数在设定级别的90%-120%范围内时，按该级别处理
- **智能匹配**：系统自动选择最匹配的并发级别配置
- **平滑过渡**：避免因并发数微小变化导致的QPS跳变

## 性能特性

- **响应时间**: 正常情况下响应时间 < 10ms
- **QPS精度**: 误差控制在5%以内
- **并发支持**: 支持数千并发连接
- **资源消耗**: 内存占用 < 512MB，CPU使用率可控

## 注意事项

1. **并发控制**: 并发数由调用方控制，我们的服务只负责检测并发级别并实施QPS限流
2. **QPS配置**: 确保配置的QPS不超过系统硬件能力
3. **并发级别配置**: 合理配置不同并发级别对应的目标QPS，避免系统过载
4. **生产环境**: 建议启用监控和日志，定期检查系统资源使用情况

## 开发计划

- [ ] 支持动态配置更新
- [ ] 添加性能监控面板
- [ ] 支持分布式部署
- [ ] 添加更多限流算法

## 许可证

MIT License
