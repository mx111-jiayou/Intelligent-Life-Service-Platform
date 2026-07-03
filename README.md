# 智享 - 智能化生活服务平台

本项目基于传统「苍穹外卖」业务系统改造，保留商家端、用户端、菜品套餐、购物车、订单、支付、催单、统计等核心外卖业务，并逐步加入 Redis 缓存优化、全局唯一订单号、AI 客服、RAG 知识库雏形、Function Calling 订单工具、慢 SQL 监控与 SQL 优化结果缓存等能力。

项目目标是从普通外卖管理系统升级为 **餐饮外卖智能客服与性能优化平台**。

## 项目亮点

- 不脱离原始苍穹外卖业务，所有增强都围绕真实业务链路展开。
- Redis 缓存店铺状态、菜品列表、空值结果，结合 TTL 降低缓存穿透风险。
- 基于 Redis 自增生成全局唯一订单号，降低高并发下订单号冲突风险。
- 新增 AI 客服对话入口，支持知识库问答、短期记忆和长期对话记录。
- 实现 Function Calling 订单工具雏形，支持查询订单、最近订单、催单、取消订单二次确认。
- 自定义 MyBatis 慢 SQL 拦截器，采集 SQL、Mapper、耗时和 SQL 签名。
- 使用线程池异步写入慢 SQL 日志，降低同步链路耗时。
- 基于 `sqlSignature` 缓存 SQL 优化结果，避免同类慢 SQL 重复调用 AI。

## 技术栈

### 后端基础

- Java
- Spring Boot 2.7.3
- Spring MVC
- MyBatis
- Maven 多模块
- Lombok

### 数据库与缓存

- MySQL
- Redis
- Druid
- PageHelper
- RedisTemplate / StringRedisTemplate

### 接口与工程

- RESTful API
- JWT
- Knife4j / Swagger
- WebSocket
- Spring Async
- ThreadPoolTaskExecutor

### AI 与智能化能力

- AI 客服对话入口
- RAG 知识库雏形
- Redis 短期对话记忆
- MySQL 长期对话记录
- Function Calling 订单工具雏形
- AI 工具调用日志
- SQL 优化结果缓存

后续计划接入：

- Spring AI Alibaba
- DashScope
- Dify
- JSqlParser
- RabbitMQ
- Caffeine

## 模块结构

```text
sky-take-out
├── sky-common   # 公共常量、工具类、异常、返回结果、上下文
├── sky-pojo     # DTO、Entity、VO
└── sky-server   # Controller、Service、Mapper、配置、拦截器、任务
```

## 已完成能力

### 1. Redis 缓存优化

- 统一缓存 key 常量 `CacheConstant`
- 店铺营业状态缓存：`SHOP_STATUS`
- 店铺状态为空时默认返回打烊，避免空值异常
- 用户端菜品列表按分类缓存
- 菜品列表缓存加入 30 分钟 TTL
- 空菜品列表也写入缓存，减少缓存穿透导致的重复查库
- 管理端新增、修改、删除、起售/停售菜品后清理相关缓存

### 2. Redis 全局唯一订单号

原项目使用 `System.currentTimeMillis()` 生成订单号，同毫秒并发下存在冲突风险。

当前已改造为：

- 时间戳 + Redis 自增序列生成订单号
- 每天使用独立 Redis 计数 key
- 为后续秒杀、限流、异步下单打基础

核心类：

```text
sky-server/src/main/java/com/sky/utils/RedisIdWorker.java
```

### 3. AI 客服知识库

新增客服知识库模块，作为 RAG 能力的业务基础。

已支持：

- 新增知识库文档
- 修改知识库文档
- 删除知识库文档
- 启用/禁用知识库文档
- 分页查询知识库文档
- 按关键词检索启用状态的知识库文档

核心表：

```text
knowledge_doc
```

核心接口：

```text
POST /admin/knowledge-doc
PUT  /admin/knowledge-doc
GET  /admin/knowledge-doc/page
GET  /admin/knowledge-doc/search
POST /admin/knowledge-doc/status/{status}
```

### 4. AI 客服对话入口与记忆

新增用户端 AI 客服接口：

```text
POST /user/ai/chat
```

当前能力：

- 保存用户消息和助手回复
- MySQL 落库长期对话记录
- Redis 保存短期会话记忆
- 基于知识库检索结果生成基础客服回复
- 后续可替换为 Spring AI Alibaba / DashScope 生成式回复

核心表：

```text
chat_message
```

### 5. Function Calling 订单工具雏形

AI 客服当前支持本地规则版工具调用：

- 查询最近订单
- 查询指定订单
- 催单
- 取消订单
- 取消订单二次确认
- 记录工具调用日志

核心表：

```text
ai_tool_call_log
```

示例：

```text
用户：查询最近订单
系统：调用 query_latest_order 工具

用户：催单 123
系统：调用 remind_order 工具

用户：取消订单 123
系统：提示二次确认

用户：确认取消订单 123
系统：调用 cancel_order 工具
```

### 6. MyBatis 慢 SQL 监控

新增自定义 MyBatis 拦截器：

- 拦截 MyBatis `SELECT` 查询
- 统计 SQL 执行耗时
- 超过阈值后输出 warn 日志
- 生成归一化 SQL
- 生成 `sqlSignature`
- 使用线程池异步写入慢 SQL 日志表

默认阈值：

```yaml
sky:
  sql-monitor:
    slow-threshold-ms: ${SKY_SQL_MONITOR_SLOW_THRESHOLD_MS:500}
```

核心类：

```text
sky-server/src/main/java/com/sky/interceptor/SlowSqlInterceptor.java
```

核心表：

```text
slow_sql_log
```

核心接口：

```text
GET /admin/slow-sql/page
GET /admin/slow-sql/signature/{sqlSignature}
```

### 7. SQL 优化结果缓存

基于慢 SQL 的 `sqlSignature` 保存优化结果：

- 原始 SQL
- 优化后 SQL
- 优化建议
- 评分
- 模型名称
- 创建时间 / 更新时间

同时使用 Redis 缓存优化结果，避免同类慢 SQL 重复调用 AI。

核心表：

```text
sql_optimization_result
```

核心接口：

```text
POST /admin/sql-optimization/result
GET  /admin/sql-optimization/result/{sqlSignature}
```

## 数据库脚本

新增能力相关脚本位于：

```text
sql/
├── ai_tool_call_log.sql
├── chat_message.sql
├── knowledge_doc.sql
├── slow_sql_log.sql
└── sql_optimization_result.sql
```

## 运行说明

### 环境要求

- JDK 8+
- Maven
- MySQL
- Redis

### 修改配置

主要配置文件：

```text
sky-server/src/main/resources/application.yml
```

MySQL 默认配置：

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

### 构建项目

```bash
mvn clean test
```

### 启动服务

```bash
cd sky-server
mvn spring-boot:run
```

默认端口：

```text
8080
```

## 后续规划

1. 采集 SQL 执行上下文，包括接口路径、用户、参数摘要、traceId。
2. 接入 JSqlParser，解析 SQL 表名、字段、where、order by、join。
3. 建立 SQL 性能评分体系，识别 `select *`、索引失效、全表扫描等问题。
4. 接入 Easy Rules，实现规则化 SQL 问题判定。
5. 接入 Spring AI Alibaba / DashScope，将本地规则回复升级为大模型回复。
6. 将知识库检索从 SQL like 升级为向量检索。
7. 使用大模型 Function Calling 替换本地关键词意图识别。
8. 自动生成 SQL 优化建议，并优先读取 Redis 缓存结果。
9. 接入 RabbitMQ，异步处理 AI 优化任务和消息通知。
10. 扩展商家经营分析、用户推荐、配送调度等智能业务。

## 项目定位总结

传统苍穹外卖解决的是外卖业务能否跑通。

本项目进一步解决：

- 业务高频数据如何缓存
- 订单号如何高并发唯一
- 用户客服问题如何自动回答
- 订单操作如何由自然语言触发
- 慢 SQL 如何自动发现、记录和复用优化建议

因此，本项目可以作为 **Java 后端 + AI 应用工程 + 性能优化** 方向的综合实践项目。
