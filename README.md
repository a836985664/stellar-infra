# stellar-infra

基于 Spring Cloud 的微服务实战项目，涵盖服务注册与配置中心、网关路由、统一鉴权、AOP 操作日志、WebSocket 实时通信、AI 语音对话等能力。作为个人学习与面试展示项目，同时也是对 Spring Cloud Alibaba / Spring AI / Redis / RabbitMQ 等中间件的实践落地。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 25 |
| 框架 | Spring Boot 3.4.5、Spring Cloud 2024.0.1 |
| 注册/配置中心 | Nacos 3.2.3 |
| 网关 | Spring Cloud Gateway |
| 数据访问 | Spring Data JPA + PostgreSQL |
| 缓存/锁 | Redis + Redisson |
| 消息队列 | RabbitMQ |
| 认证 | JWT、Spring Security |
| AI | Spring AI（Ollama）、ChatTTS |
| 文档 | SpringDoc OpenAPI（Swagger）|
| 部署 | Docker + Docker Compose |

## 项目结构

```
stellar-infra
├── user-service      # 用户服务：注册、微信登录、AI 对话、语音识别、操作日志
├── tts-service       # 语音合成服务：对接 ChatTTS（Python 子进程）
├── gateway-service   # 网关：统一入口、动态路由、JWT 鉴权、跨域
├── stellar-common    # 公共模块：通用异常、工具类、JWT 组件
├── nacos             # Nacos 部署相关（含 PostgreSQL 数据源插件）
├── docker-compose.yml # 中间件编排（Nacos / MySQL / RabbitMQ）
└── compose.yml       # 服务容器化编排
```

## 模块说明

### user-service（端口 8082）

核心业务服务，主要能力：

- **用户模块**：用户 CRUD、微信扫码登录（二维码生成 / 状态轮询 / 手机授权）
- **AI 对话**：基于 Spring AI + Ollama，实现智能客服对话，支持关键词转人工
- **语音识别（STT）**：对接语音识别服务
- **实时通信**：WebSocket 聊天（ChatWebSocketHandler）
- **操作日志**：自定义 `@OperationLog` 注解 + AOP 环绕通知，统一记录接口入参、操作人、IP、耗时、结果到数据库
- **定时任务**：基于 Spring Schedule
- **全局异常处理**：统一异常捕获与响应

### tts-service（端口 8083）

- 对接 ChatTTS（Python），通过子进程调用实现文本转语音
- 路径等依赖通过环境变量外置，便于跨机器部署

### gateway-service（端口 8081）

- 统一入口，基于服务发现（`lb://`）动态路由到 user-service / tts-service
- 全局跨域配置
- JWT 统一鉴权（`security.gateway.enabled=true`，支持白名单）
- Actuator 监控端点

### stellar-common

- 通用异常体系
- 公共工具与 JWT 相关组件下沉

## 快速开始

### 1. 启动中间件

```bash
# 启动 Nacos、MySQL、RabbitMQ
docker-compose up -d
```

> Nacos 默认使用 PostgreSQL 作为数据源（见 `nacos/conf/application.properties`）。

### 2. 启动服务

按顺序启动各模块（在 IDE 中运行对应 `*Application.java`）：

1. `user-service` → `UserServiceApplication`
2. `tts-service` → `TtsServiceApplication`
3. `gateway-service` → `GatewayApplication`

### 3. 访问

| 入口 | 地址 |
|------|------|
| 网关 | http://localhost:8081 |
| Swagger（各服务） | http://localhost:8082/swagger-ui.html |
| Nacos 控制台 | http://localhost:8848/nacos |
| RabbitMQ 管理台 | http://localhost:15672 |

## 功能亮点

- **AOP 操作日志**：自定义注解 + 环绕通知，业务代码零侵入，通过 MQ 异步落库（解耦 / 削峰）
- **微服务治理**：Nacos 注册 + 配置中心，Gateway 动态路由 + 统一鉴权
- **可靠消息**：RabbitMQ 持久化 + 手动 ACK + 死信队列兜底
- **实时通信**：WebSocket 实现在线客服
- **AI 集成**：Spring AI 对接 Ollama，ChatTTS 语音合成

## 环境要求

- JDK 25
- Maven 3.6+
- Docker（用于中间件，可选）
- PostgreSQL / Redis / RabbitMQ / Nacos