# TrainTicketing 🚄

高仿 12306 火车票购票系统 —— 以铁路购票业务为载体的**分布式高并发学习项目**。

基于 Spring Boot 3 + Spring Cloud 微服务架构，围绕「会员 → 购票 → 订单 → 支付」完整链路，逐步落地缓存、消息队列、分布式锁、分库分表等核心技术。

> 前身：12306-train-system（2024-02 原型工程）。2026-08 重启并标准化：项目重命名、模块前缀统一、升级 JDK21、重写 DDL、补充开源规范。

## ✨ Roadmap

| 阶段 | 内容 | 状态 |
|---|---|---|
| 阶段 0 | 工程地基：统一异常/返回/日志 AOP、会员注册/登录闭环 | ✅ 基础完成（短信通道为占位实现） |
| 阶段 1 | 单体核心域：车站/车次/车厢/座位/每日排班/**区间占用余票模型**/订单 | ⬜ 待开始 |
| 阶段 2 | 高并发三板斧：Redis 余票缓存 + Lua 预扣、Redisson 细粒度分布式锁、MQ 削峰 + 延时关单 | ⬜ 待开始 |
| 阶段 3 | 微服务化：user/ticket/order/pay 服务拆分 + Nacos + Sentinel + 分库分表 | ⬜ 待开始 |

## 🛠 技术栈

- JDK 21 / Spring Boot 3.3 / Spring Cloud 2023.0.x
- Spring Cloud Gateway（网关）
- MyBatis + MySQL 8（InnoDB / utf8mb4 / 雪花 ID）
- Vue 3 + Ant Design Vue（前端）
- 规划中：Redis / RocketMQ / ShardingSphere / Nacos / Sentinel

## 📁 模块结构

| 模块 | 端口 | 说明 |
|---|---|---|
| train-ticketing-gateway | 8000 | 网关：路由转发（规划限流/鉴权） |
| train-ticketing-member | 8001 | 会员服务：注册/短信验证码/登录/乘车人 |
| train-ticketing-common | - | 公共模块：统一返回、异常处理、日志 AOP |
| train-ticketing-generator | - | MyBatis Generator 代码生成器 |
| web | 9000 | 前端（Vue 3 + Ant Design Vue） |

## 🚀 快速开始

1. 依赖：JDK 21、Maven 3.9+、MySQL 8（前端需要 Node 16+）。
2. 初始化数据库：执行 `script/sql/train-ticketing.sql`，自动建库 `train_ticketing` 并创建全部表。
3. 修改 `train-ticketing-member/src/main/resources/application.properties` 中的数据库账号密码。
4. 启动后端：
   ```bash
   # 会员服务（8001）
   mvn spring-boot:run -pl train-ticketing-member
   # 网关（8000）
   mvn spring-boot:run -pl train-ticketing-gateway
   ```
5. 启动前端：
   ```bash
   cd web && yarn && yarn serve
   ```
6. 验证：`curl http://localhost:8000/member/member/count`

接口调试文件见 `http/member-member.http`（IDEA HTTP Client 直接运行）。

## 🌿 分支规范

- `master`：受保护主干，只接受 PR 合入，禁止直接推送。
- `feat/*`：功能迭代分支（如 `feat/order-service`、`feat/redis-ticket-cache`）。
- `fix/*`：缺陷修复分支（如 `fix/member-login-bug`）。
- 流程：从 `master` 迁出 `feat/*` → 开发自测 → PR 合回 `master`。

## 🗄 数据库设计

见 `script/sql/train-ticketing.sql`（13 张表，全量注释）。

核心是**区间占用余票模型**：12306 的余票不是「车次总票数 - 已售」，而是一张 A→C 的票同时占用 A-B、B-C 两个区间，某区间余票 = 该区间内未被售出的座位数。`train_order_item` 通过 `depart_index / arrive_index` 记录每个座位的占用区间，是后续防超卖与座位分配算法的基础。

## 📚 文档

- [架构说明](docs/architecture.md)
- [数据库设计](docs/database.md)
- [开发规范](AGENTS.md)

## 📄 License

[Apache-2.0](LICENSE)

## 🙏 参考与致谢

- [nageoffer/12306](https://github.com/nageoffer/12306)：同题材高并发学习项目（Apache-2.0），架构设计思路参考来源。
