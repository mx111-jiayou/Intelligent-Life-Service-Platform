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

### 5. 慢 SQL 异步落库与管理查询

已完成：

- 新增慢 SQL 日志表 `slow_sql_log`
- 慢 SQL 拦截器生成归一化 SQL 与 `sqlSignature`
- 使用线程池异步写入慢 SQL 日志，降低同步链路耗时
- 新增管理端慢 SQL 分页查询接口
- 支持按 `sqlSignature` 查询同类慢 SQL

核心接口：

```text
GET /admin/slow-sql/page
GET /admin/slow-sql/signature/{sqlSignature}
```

### 6. AI 客服知识库与对话入口

已完成：

- 新增客服知识库表 `knowledge_doc`
- 支持管理端新增、修改、删除、启停、分页查询知识库文档
- 支持按关键词检索启用状态的知识库文档
- 新增用户端 AI 客服对话接口
- 新增 `chat_message` 对话记录表
- 使用 Redis 保存短期会话记忆，MySQL 沉淀长期对话记录
- AI 客服可基于知识库内容生成基础回复

核心接口：

```text
POST /user/ai/chat
GET /admin/knowledge-doc/page
GET /admin/knowledge-doc/search
```

### 7. Function Calling 订单工具雏形

已完成：

- AI 客服可识别“最近订单”意图并调用本地订单服务
- AI 客服可识别“取消订单”意图并调用本地订单取消能力
- AI 客服可识别“查询指定订单”“催单”等订单意图
- 对取消订单增加二次确认提示，降低误操作风险
- 新增 AI 工具调用日志表 `ai_tool_call_log`，记录工具名称、参数、结果和成功状态
- 新增订单查询工具方法，支持获取用户最近一笔订单摘要
- 修正下单时 `order_time` 未写入的问题，保证最近订单排序准确

当前属于 Function Calling 的本地规则版雏形，后续接入 Spring AI Alibaba / DashScope 后，可替换为大模型工具调用。

### 8. SQL 优化结果缓存

已完成：

- 新增 SQL 优化结果表 `sql_optimization_result`
- 基于 `sqlSignature` 保存优化建议、优化后 SQL、评分和模型名称
- 使用 Redis 缓存 SQL 优化结果，避免相同慢 SQL 重复调用 AI
- 新增管理端保存/查询 SQL 优化结果接口

核心接口：

```text
POST /admin/sql-optimization/result
GET /admin/sql-optimization/result/{sqlSignature}
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

1. SQL 执行上下文采集，包括接口路径、用户、参数摘要
2. JSqlParser SQL 结构解析
3. SQL 性能评分体系
4. Easy Rules 规则引擎集成
5. Spring AI Alibaba / DashScope 接入
6. RAG 向量检索增强知识库问答
7. 大模型 Function Calling 替换本地意图规则
8. AI 生成 SQL 优化建议
9. RabbitMQ 异步处理 AI 优化任务
10. 商家经营分析、用户推荐、配送调度等智能业务扩展

## 项目亮点

- 不脱离原始苍穹外卖业务，所有增强都围绕真实业务链路展开
- 缓存优化、订单号生成、慢 SQL 识别、AI 客服入口均可实际运行
- 已具备 RAG 知识库、Redis 短期记忆、MySQL 长期对话记录、订单工具调用和工具调用日志
- 已具备 SQL 优化结果缓存能力，可复用同类慢 SQL 的优化建议
- 改造路径清晰，适合作为课程项目升级、简历项目或毕业设计基础
- 后续可自然扩展到 AI SQL 优化、智能经营分析和智能推荐系统
