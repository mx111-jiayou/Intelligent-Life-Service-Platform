# 智享 - 智能化生活服务平台

本项目基于传统「苍穹外卖」业务系统改造，保留商家端、用户端、订单、菜品、套餐、购物车、地址、支付、统计等原始业务能力，并逐步加入 Redis 缓存优化、全局唯一订单号、慢 SQL 识别、SQL 性能分析与 AI 优化建议等能力，目标是从普通外卖管理系统升级为智能化餐饮生活服务平台。

## 项目定位

传统苍穹外卖主要解决外卖业务流程是否能跑通，包括商家管理、用户点餐、购物车、下单、支付和订单处理。

本项目在原业务基础上继续增强：

- 提升高并发场景下的缓存访问效率
- 降低订单号生成冲突风险
- 自动识别业务系统中的慢 SQL
- 为后续 SQL 评分、AI 诊断、RAG 知识库优化打基础
- 逐步扩展商家经营分析、用户推荐、配送调度等智能化能力

## 技术栈

- Spring Boot 2.7.3
- MyBatis
- MySQL
- Redis
- PageHelper
- Druid
- Knife4j / Swagger
- WebSocket
- Maven 多模块工程

后续规划接入：

- RabbitMQ
- JSqlParser
- Spring AI Alibaba
- DashScope
- Dify
- RAG 知识库

## 模块结构

```text
sky-take-out
├── sky-common   # 公共常量、工具类、异常、返回结果
├── sky-pojo     # DTO、Entity、VO
└── sky-server   # 控制器、业务服务、Mapper、配置、拦截器
```

## 已完成改造

### 1. 项目基线整理

完成 Maven 多模块项目基线提交，确认项目可以通过 Maven 构建。

验证命令：

```bash
mvn clean test
```

### 2. Redis 缓存基线优化

已完成：

- 新增统一缓存常量 `CacheConstant`
- 店铺营业状态统一使用 Redis key：`SHOP_STATUS`
- 店铺状态为空时默认返回打烊状态，避免空值导致异常
- 用户端菜品列表按分类缓存
- 菜品列表缓存加入 30 分钟 TTL
- 空菜品列表也写入缓存，减少缓存穿透导致的重复查库
- 管理端新增、修改、删除、起售/停售菜品后清理相关缓存

涉及能力：

- 缓存穿透基础防护
- 缓存 key 统一管理
- 缓存数据一致性维护

### 3. Redis 全局唯一订单号

传统实现使用 `System.currentTimeMillis()` 生成订单号，在高并发或同毫秒下单场景中存在冲突风险。

当前已改造为 Redis 全局 ID：

- 使用时间戳 + Redis 自增序列生成订单号
- 每天使用独立 Redis 计数 key
- 降低单体环境下订单号重复风险
- 为后续秒杀、限流、异步下单打基础

核心类：

```text
sky-server/src/main/java/com/sky/utils/RedisIdWorker.java
```

### 4. MyBatis 慢 SQL 拦截器

已新增 MyBatis 慢 SQL 识别能力：

- 拦截 MyBatis `SELECT` 查询
- 统计 SQL 执行耗时
- 超过阈值后输出 warn 日志
- 默认慢 SQL 阈值：`500ms`
- 支持通过环境变量调整阈值

配置项：

```yaml
sky:
  sql-monitor:
    slow-threshold-ms: ${SKY_SQL_MONITOR_SLOW_THRESHOLD_MS:500}
```

核心类：

```text
sky-server/src/main/java/com/sky/interceptor/SlowSqlInterceptor.java
```

## 运行说明

### 1. 准备环境

需要本地准备：

- JDK 8+
- Maven
- MySQL
- Redis

### 2. 修改配置

主要配置文件：

```text
sky-server/src/main/resources/application.yml
```

数据库默认配置：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/sky_take_out
      username: root
      password:
```

Redis 默认配置：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

### 3. 构建项目

```bash
mvn clean test
```

### 4. 启动服务

```bash
cd sky-server
mvn spring-boot:run
```

默认端口：

```text
8080
```

## 改造路线

后续按以下顺序继续推进：

1. 慢 SQL 日志表落库
2. 慢 SQL 管理端查询接口
3. SQL 执行上下文采集，包括 Mapper、接口路径、耗时、参数
4. JSqlParser SQL 结构解析
5. SQL 性能评分体系
6. Easy Rules 规则引擎集成
7. Dify / Spring AI Alibaba 接入
8. RAG 知识库接入
9. AI 生成 SQL 优化建议
10. 商家经营分析、用户推荐、配送调度等智能业务扩展

## 项目亮点

- 不脱离原始苍穹外卖业务，所有增强都围绕真实业务链路展开
- 缓存优化、订单号生成、慢 SQL 识别均可实际运行
- 改造路径清晰，适合作为课程项目升级、简历项目或毕业设计基础
- 后续可自然扩展到 AI SQL 优化、智能经营分析和智能推荐系统
