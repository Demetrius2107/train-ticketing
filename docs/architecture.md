# 架构说明

> TrainTicketing 高仿 12306 火车票购票系统（分布式高并发学习项目）。本文档描述当前架构与演进路线。

## 1. 整体架构

```
┌──────────┐   HTTP    ┌───────────────┐   Route    ┌─────────────────────┐
│  web     │ ────────▶ │   gateway     │ ─────────▶ │   train-ticketing-   │
│ (Vue 3)  │   :9000   │   :8000       │  /member/**│   member  :8001      │
└──────────┘           └───────────────┘            └──────────┬──────────┘
                                                                │ MyBatis
                                                        ┌───────▼────────┐
                                                        │ MySQL train_   │
                                                        │ ticketing      │
                                                        └────────────────┘
```

当前为**单体雏形 + 网关**：只有会员服务（member）。阶段 1 将把车站/车次/座位/订单等业务域加进 member 或以新模块承载；阶段 3 再按 用户/购票/订单/支付 拆分微服务。

## 2. 模块职责

| 模块 | 端口 | 职责 |
|---|---|---|
| train-ticketing-gateway | 8000 | 统一入口：路由转发（`/member/**` → 8001），规划中：限流/鉴权/CORS |
| train-ticketing-member | 8001 | 会员域：注册、短信验证码（占位）、登录、乘车人管理（context-path `/member`） |
| train-ticketing-common | - | 公共：`CommonResp` 统一返回、`BusinessException`/`BusinessExceptionEnum`、日志 AOP |
| train-ticketing-generator | - | MyBatis Generator 代码生成（不参与运行） |
| web | 9000 | 前端（Vue 3 + Ant Design Vue） |

## 3. 请求链路

1. 浏览器访问 `http://localhost:9000`（web 开发服务器）
2. 前端请求 `http://localhost:8000/member/member/count`
3. Gateway 按 `Path=/member/**` 转发到 member 服务（8001）
4. member Controller → Service → Mapper → MySQL `train_ticketing` 库
5. 响应统一 `CommonResp{success, message, content}`；异常由全局 `ControllerExceptionHandler` 归一

## 4. 端口与配置

| 项 | 值 |
|---|---|
| gateway | 8000（`gateway/src/main/resources/application.properties`） |
| member | 8001，context-path `/member` |
| web | 9000（`web/package.json` scripts.serve） |
| MySQL | `localhost:3306/train_ticketing`，root/123456（开发环境） |

## 5. 演进路线

- **阶段 1（当前）**：单体核心域。车站/车次/车厢/座位/票价/每日排班 → 区间占用余票 → 订单（下单/支付过期取消）。表结构已就绪（`script/sql/train-ticketing.sql`）。
- **阶段 2**：Redis 余票缓存（管道批量取）+ Lua 预扣、Redisson 细粒度锁（车次+日期+座位类型）、MQ 异步下单削峰 + 延时消息关单、令牌桶限流。
- **阶段 3**：服务拆分 user/ticket/order/pay + Nacos + Sentinel + ShardingSphere 分库分表。

## 6. 相关文档

- [README](../README.md) — 快速开始 / Roadmap
- [数据库设计](database.md)
- [AGENTS.md](../AGENTS.md) — 开发规范
