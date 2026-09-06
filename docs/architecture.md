# 架构说明

> TrainTicketing 高仿 12306 火车票购票系统（分布式高并发学习项目）。本文档描述当前架构与请求链路。
> 交互版架构图见 [diagrams/architecture.html](diagrams/architecture.html)。

## 1. 整体架构

```
                     ┌──────────────────────────────────────────────────┐
                     │                docker compose 网络                │
┌──────────┐  HTTP   │ ┌──────────────┐  Route     ┌──────────────────┐ │
│  web     │───────▶ │ │   gateway    │──────────▶ │ train-ticketing- │ │
│ (Vue 3)  │  :9000  │ │  :8000 JWT   │ /member/** │ member  :8001    │ │
└──────────┘         │ └──────┬───────┘ /business/**└──────────────────┘ │
                     │        │ /business/**                             │
                     │ ┌──────▼──────────────┐   Redisson锁/Lua预扣   ┌────────┐
                     │ │ train-ticketing-    │◀─────────────────────▶│ Redis 7 │
                     │ │ business  :8002     │   余票缓存+幂等        └────────┘
                     │ │ 订单域+出票消费者     │
                     │ └──┬───────────┬──────┘        RocketMQ 4.9.4
                     │    │ MyBatis   │  发消息/消费      ┌───────────────┐
                     │    │           ├─────────────────▶│ namesrv:9876  │
                     │    │           │  出票消息/延时关单 │ broker :10911 │
                     │ ┌──▼───────┐   │                  └───────────────┘
                     │ │ MySQL 8  │◀──┘   （压测/监控：--profile monitoring
                     │ │ train_   │        Prometheus:9090 / Grafana:3000 / 控制台:18080）
                     │ │ ticketing│
                     │ └──────────┘
                     └──────────────────────────────────────────────────┘
```

单体雏形 + 网关：member（会员域）与 business（核心购票域）两个服务，Redis/MySQL/RocketMQ 三件中间件。
阶段 3 计划按 用户/购票/订单/支付 拆分微服务并引入 Nacos + ShardingSphere。

## 2. 模块职责

| 模块 | 端口 | 职责 |
|---|---|---|
| train-ticketing-gateway | 8000 | 统一入口：路由转发 + JWT 校验（`AuthFilter` 校验后注入 `X-Member-Id`） |
| train-ticketing-member | 8001 | 会员域：注册、短信验证码（容器 mock 直返）、登录、乘车人（context-path `/member`） |
| train-ticketing-business | 8002 | 核心域：车站/车次/座位/排班/余票/订单；同步下单 + MQ 异步出票消费者 + 延时关单 + 对账（context-path `/business`） |
| train-ticketing-common | - | 公共：`CommonResp`、`BusinessException`/`BusinessExceptionEnum`、日志 AOP |
| web | 9000 | 前端（Vue 3 + Ant Design Vue），订单页对「出票中」订单轮询 |

## 3. 请求链路

**通用链路**：web(9000) → gateway(8000)（JWT 校验，注入 `X-Member-Id`）→ member/business → Service → Mapper → MySQL；响应统一 `CommonResp{success, message, content}`，业务异常由 `ControllerExceptionHandler` 归一。

**同步下单**（保留，压测脚本默认）：`POST /business/order/save`
幂等（Redis SETNX）→ 锁外校验 → Redisson 锁（`ticket:lock:{排班}:{座位类型}`）→ 事务【Lua 预扣 → 行锁选座+贪心分配 → 落订单/明细】→ 提交后发延时关单消息。

**异步下单（MQ 削峰，主链路）**：`POST /business/order/async`
请求线程只做 幂等（存预生成单号）→ 校验 → **Lua 预扣** → 预插「4出票中」订单 → 同步发消息 → 毫秒级返回单号；
消费者（并发 20）拿同一把 Redisson 锁 → 消费幂等 → 行锁选座+插明细 → CAS 出票中→待支付 → afterCommit 发延时关单消息；
前端订单页对「出票中」订单 3s 轮询至终态（0待支付 / 5出票失败）。

**可靠性闭环**：
- 生产者：syncSend 失败当场补偿（置出票失败 + 回补预扣余票）；
- 消费者：失败分型——余票耗尽=确定性失败（终态化+回补，不重试）；锁忙/DB 抖动=临时失败（RocketMQ 递增间隔重试 5 次后进死信）；
- 兜底扫描（每 5 分钟）：PENDING 过期关单（延时消息丢失时接管）+ 出票中悬挂单收敛（按 Redis 预扣上下文回补）；
- 延时关单：10 分钟延时消息（等级可配，对齐 `expire_time`）→ CAS 待支付→已取消（与支付竞态天然安全）→ 删明细 → afterCommit 回补余票。

**兜底正确性**：整点对账任务以 DB 区间占用模型重建余票缓存（`TicketReconcileService`）。

## 4. 端口与配置

| 项 | 值 |
|---|---|
| gateway | 8000 |
| member | 8001，context-path `/member` |
| business | 8002，context-path `/business` |
| web | 9000 |
| MySQL | 容器宿主映射 13306（容器内 3306），root/123456 |
| Redis | 6379（免密） |
| RocketMQ | namesrv 9876（宿主可连），broker 10911（仅容器网络内，`brokerIP1=服务名`） |
| Grafana / Prometheus / RocketMQ 控制台 | 3000 / 9090 / 18080（`--profile monitoring`） |

## 5. 演进路线

- ~~阶段 1~~ ✅ 单体核心域（区间占用余票模型 / 贪心选座）。
- ~~阶段 2~~ ✅ Redis 余票缓存 + Lua 预扣、Redisson 细粒度锁、RocketMQ 异步下单削峰 + 延时消息关单 + 兜底扫描。
- **阶段 3（下一步）**：下单锁粒度优化（按车厢分段锁）、list 接口 N+1 治理 → 服务拆分 user/ticket/order/pay + Nacos + Sentinel + ShardingSphere 分库分表。

## 6. 相关文档

- [README](../README.md) — 快速开始 / Roadmap
- [数据库设计](database.md)
- [压测报告 2026-09](load-test-report-2026-09.md)
- [AGENTS.md](../AGENTS.md) — 开发规范
