# JVM监控配置文档

## 概述

项目已集成Spring Boot Actuator和Micrometer Prometheus，提供完整的JVM监控功能。

---

## 一、依赖说明

### 已添加的依赖

1. **spring-boot-starter-actuator**
   - Spring Boot官方监控和管理工具
   - 提供健康检查、指标收集等功能

2. **micrometer-registry-prometheus**
   - Prometheus指标导出
   - 支持Prometheus格式的指标数据

3. **micrometer-core**
   - 指标收集核心库
   - 提供JVM、系统、应用等指标

---

## 二、监控端点

### 基础端点

| 端点 | 路径 | 说明 |
|------|------|------|
| Health | `/timetable/api/actuator/health` | 应用健康状态 |
| Info | `/timetable/api/actuator/info` | 应用信息 |
| Metrics | `/timetable/api/actuator/metrics` | 所有指标列表 |
| Prometheus | `/timetable/api/actuator/prometheus` | Prometheus格式指标 |

### JVM监控端点

| 端点 | 路径 | 说明 |
|------|------|------|
| Thread Dump | `/timetable/api/actuator/threaddump` | 线程转储 |
| Heap Dump | `/timetable/api/actuator/heapdump` | 堆转储（下载文件） |

### 配置端点

| 端点 | 路径 | 说明 |
|------|------|------|
| Environment | `/timetable/api/actuator/env` | 环境变量 |
| Beans | `/timetable/api/actuator/beans` | Spring Bean列表 |
| Config Props | `/timetable/api/actuator/configprops` | 配置属性 |

---

## 三、JVM监控指标

### 内存指标

```
jvm.memory.used          # 已使用内存（字节）
jvm.memory.committed     # 已提交内存（字节）
jvm.memory.max           # 最大内存（字节）
jvm.memory.usage         # 内存使用率（0-1）
```

**内存区域**：
- `heap` - 堆内存
- `nonheap` - 非堆内存
- `heap.used` - 堆已使用
- `heap.committed` - 堆已提交
- `heap.max` - 堆最大值

### GC指标

```
jvm.gc.pause            # GC暂停时间（毫秒）
jvm.gc.collections       # GC收集次数
```

**GC类型**：
- `jvm.gc.pause[action=end of minor GC]` - Minor GC
- `jvm.gc.pause[action=end of major GC]` - Major GC

### 线程指标

```
jvm.threads.live         # 当前活动线程数
jvm.threads.daemon       # 守护线程数
jvm.threads.peak        # 峰值线程数
jvm.threads.states       # 线程状态统计
```

### 类加载指标

```
jvm.classes.loaded       # 已加载类数
jvm.classes.unloaded     # 已卸载类数
```

### 系统指标

```
system.cpu.usage         # CPU使用率（0-1）
system.cpu.count         # CPU核心数
process.cpu.usage        # 进程CPU使用率
process.uptime           # 进程运行时间（秒）
```

---

## 四、使用示例

### 1. 查看健康状态

```bash
curl http://localhost:8089/timetable/api/actuator/health
```

**响应示例**：
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

### 2. 查看所有指标

```bash
curl http://localhost:8089/timetable/api/actuator/metrics
```

### 3. 查看特定指标

```bash
# 查看堆内存使用
curl http://localhost:8089/timetable/api/actuator/metrics/jvm.memory.used?tag=area:heap

# 查看GC暂停时间
curl http://localhost:8089/timetable/api/actuator/metrics/jvm.gc.pause

# 查看线程数
curl http://localhost:8089/timetable/api/actuator/metrics/jvm.threads.live
```

### 4. 获取Prometheus格式指标

```bash
curl http://localhost:8089/timetable/api/actuator/prometheus
```

**响应示例**：
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="PS Survivor Space"} 1.048576E7
jvm_memory_used_bytes{area="heap",id="PS Old Gen"} 2.097152E8
jvm_memory_used_bytes{area="heap",id="PS Eden Space"} 1.048576E8
jvm_memory_used_bytes{area="nonheap",id="Metaspace"} 5.24288E7
jvm_memory_used_bytes{area="nonheap",id="Code Cache"} 1.048576E7
jvm_memory_used_bytes{area="nonheap",id="Compressed Class Space"} 1.048576E7

# HELP jvm_threads_live_threads The current number of live threads
# TYPE jvm_threads_live_threads gauge
jvm_threads_live_threads 45

# HELP jvm_gc_pause_seconds Time spent in GC pause
# TYPE jvm_gc_pause_seconds summary
jvm_gc_pause_seconds_count{action="end of minor GC",cause="Allocation Failure"} 123
jvm_gc_pause_seconds_sum{action="end of minor GC",cause="Allocation Failure"} 0.456
```

### 5. 获取线程转储

```bash
curl http://localhost:8089/timetable/api/actuator/threaddump
```

### 6. 下载堆转储

```bash
curl -O http://localhost:8089/timetable/api/actuator/heapdump
```

---

## 五、集成Prometheus和Grafana

### 1. Prometheus配置

在`prometheus.yml`中添加：

```yaml
scrape_configs:
  - job_name: 'timetable-backend'
    metrics_path: '/timetable/api/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8089']
        labels:
          application: 'timetable-backend'
          environment: 'dev'
```

### 2. Grafana仪表板

可以使用以下JVM监控仪表板：
- **JVM (Micrometer)**: ID 4701
- **Spring Boot 2.1 Statistics**: ID 11378

### 3. 常用查询

**堆内存使用率**：
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

**GC频率**：
```promql
rate(jvm_gc_pause_seconds_count[5m])
```

**线程数趋势**：
```promql
jvm_threads_live_threads
```

**CPU使用率**：
```promql
system_cpu_usage * 100
```

---

## 六、监控指标说明

### 关键指标

1. **内存使用率**
   - 正常范围：< 80%
   - 警告：80% - 90%
   - 危险：> 90%

2. **GC频率**
   - Minor GC：应该 < 1次/秒
   - Major GC：应该 < 1次/分钟

3. **线程数**
   - 正常：< 200
   - 警告：200 - 500
   - 危险：> 500

4. **CPU使用率**
   - 正常：< 70%
   - 警告：70% - 85%
   - 危险：> 85%

---

## 七、配置说明

### application.yml配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,beans,configprops,httptrace,threaddump,heapdump
      base-path: /actuator
  metrics:
    export:
      prometheus:
        enabled: true
    jvm:
      enabled: true
      memory:
        enabled: true
      threads:
        enabled: true
      gc:
        enabled: true
```

### 安全配置

监控端点已在`SecurityConfig`中配置为允许访问：
```java
.antMatchers("/actuator/**").permitAll()
```

---

## 八、生产环境建议

### 1. 安全加固

生产环境建议对监控端点进行认证：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  security:
    enabled: true
```

### 2. 性能考虑

- 堆转储文件较大，建议限制访问
- 线程转储可能影响性能，谨慎使用
- 定期清理历史指标数据

### 3. 告警配置

建议配置以下告警规则：
- 内存使用率 > 85%
- GC频率过高
- 线程数异常增长
- CPU使用率 > 80%

---

## 九、故障排查

### 常见问题

1. **端点无法访问**
   - 检查SecurityConfig配置
   - 确认端点已暴露

2. **指标数据为空**
   - 检查Micrometer配置
   - 确认JVM指标已启用

3. **Prometheus无法抓取**
   - 检查网络连接
   - 确认路径正确

---

## 十、相关资源

- [Spring Boot Actuator文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer文档](https://micrometer.io/docs)
- [Prometheus文档](https://prometheus.io/docs/)
- [Grafana仪表板](https://grafana.com/grafana/dashboards/)

---

**配置完成时间**: 2024年  
**版本**: v1.0  
**维护者**: 开发团队

