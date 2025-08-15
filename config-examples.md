# API Runner 配置示例

## QPS控制方式配置

### 1. 延迟模式（默认推荐）

```yaml
# application.yml
api:
  qps-control-enabled: true
  qps-control-mode: DELAY
  default-qps: 1000
  qps-tolerance: 5.0
```

**特点：**
- 通过 `Thread.sleep()` 增加延迟
- 响应时间可控，用户体验好
- 资源消耗低
- 适合大多数场景

### 2. CPU消耗模式

```yaml
# application.yml
api:
  qps-control-enabled: true
  qps-control-mode: CPU
  default-qps: 1000
  qps-tolerance: 5.0
  cpu-loop-count: 15000  # 调整CPU消耗强度
```

**特点：**
- 通过CPU密集型计算消耗时间
- 响应时间更精确
- 会占用更多CPU资源
- 适合对延迟精度要求极高的场景

### 3. 混合配置示例

```yaml
# application.yml
api:
  qps-control-enabled: true
  qps-control-mode: DELAY  # 默认使用延迟模式
  default-qps: 1000
  qps-tolerance: 3.0       # 更严格的误差控制
  qps-window-ms: 500       # 更短的时间窗口，响应更快
  thread-pool-size: 200    # 更大的线程池
  cpu-loop-count: 20000    # CPU模式下的循环次数
```

## URL配置示例

### 高QPS接口配置

```yaml
# urls-config.yml
urls:
  - pattern: "/api/product/*"
    qps: 2000              # 高QPS
    concurrent: 200        # 高并发
    description: "产品接口-高负载"
    enabled: true
```

### 低QPS接口配置

```yaml
# urls-config.yml
urls:
  - pattern: "/api/admin/*"
    qps: 50                # 低QPS
    concurrent: 10         # 低并发
    description: "管理接口-低负载"
    enabled: true
```

### 监控接口配置

```yaml
# urls-config.yml
urls:
  - pattern: "/actuator/*"
    qps: 100               # 适中的QPS
    concurrent: 20         # 适中的并发
    description: "监控接口"
    enabled: true
```

## 性能调优建议

### 1. 延迟模式调优

```yaml
api:
  qps-control-mode: DELAY
  qps-window-ms: 1000      # 1秒窗口，平衡精度和性能
  qps-tolerance: 5.0       # 5%误差容忍度
```

### 2. CPU模式调优

```yaml
api:
  qps-control-mode: CPU
  cpu-loop-count: 10000    # 根据CPU性能调整
  qps-tolerance: 2.0       # 更严格的误差控制
```

### 3. 生产环境配置

```yaml
api:
  qps-control-enabled: true
  qps-control-mode: DELAY  # 生产环境推荐延迟模式
  default-qps: 500         # 保守的默认QPS
  qps-tolerance: 10.0      # 宽松的误差容忍度
  thread-pool-size: 500    # 充足的线程池
  logging:
    level: WARN            # 生产环境日志级别
```

## 动态切换配置

可以通过修改配置文件并重启服务来切换QPS控制方式：

1. **从延迟模式切换到CPU模式**：
   ```yaml
   api:
     qps-control-mode: CPU
   ```

2. **从CPU模式切换回延迟模式**：
   ```yaml
   api:
     qps-control-mode: DELAY
   ```

3. **调整CPU消耗强度**：
   ```yaml
   api:
     qps-control-mode: CPU
     cpu-loop-count: 20000  # 增加循环次数，消耗更多CPU
   ```

## 监控和调试

### 1. 查看当前配置

```bash
curl http://localhost:8080/health
```

### 2. 查看QPS统计

```bash
curl "http://localhost:8080/api/stats/qps?url=/api/user/123"
```

### 3. 查看所有URL配置

```bash
curl http://localhost:8080/api/config/urls
```

### 4. 日志监控

在 `application.yml` 中启用调试日志：

```yaml
logging:
  level:
    com.example.apirunner.service.QpsControlService: DEBUG
```

这将显示详细的QPS控制过程，包括延迟时间和CPU消耗时间的计算。
