# TrainTicketing 🚄

高仿 12306 火车票购票系统 —— 以铁路购票业务为载体的**分布式高并发学习项目**。

基于 Spring Boot 3 + Spring Cloud 微服务架构，围绕「会员 → 购票 → 订单 → 支付」完整链路，逐步落地缓存、消息队列、分布式锁、分库分表等核心技术。

> 前身：12306-train-system（2024-02 原型工程）。2026-08 重启并标准化：项目重命名、模块前缀统一、升级 JDK21、重写 DDL、补充开源规范。

##  Roadmap

| 阶段 | 内容 | 状态 |
|---|---|---|
| 阶段 0 | 工程地基：统一异常/返回/日志 AOP、会员注册/登录闭环 | ✅ 基础完成（短信通道为占位实现） |
| 阶段 1 | 单体核心域：车站/车次/车厢/座位/每日排班/**区间占用余票模型**/订单 | ✅ 已完成（含贪心选座 + 卧铺铺位） |
| 阶段 2 | 高并发三板斧：Redis 余票缓存 + Lua 预扣、Redisson 细粒度分布式锁、MQ 削峰 + 延时关单 | 🔶 进行中（缓存/锁/对账/JWT/幂等/退票/CAS 已落地，缺 MQ 延时关单） |
| 阶段 3 | 微服务化：user/ticket/order/pay 服务拆分 + Nacos + Sentinel + 分库分表 | ⬜ 待开始 |

##  技术栈

- JDK 21 / Spring Boot 3.3 / Spring Cloud 2023.0.x
- Spring Cloud Gateway（网关，JWT 校验）
- MyBatis + MySQL 8（InnoDB / utf8mb4 / 雪花 ID）
- Redis 7 + Redisson（余票缓存 / Lua 预扣 / 分布式锁）
- Vue 3 + Ant Design Vue（前端）
- 规划中：RocketMQ / ShardingSphere / Nacos / Sentinel

## 模块结构

| 模块 | 端口 | 说明 |
|---|---|---|
| train-ticketing-gateway | 8000 | 网关：路由转发 + JWT 校验（规划限流） |
| train-ticketing-member | 8001 | 会员服务：注册/短信验证码/登录/乘车人 |
| train-ticketing-business | 8002 | 业务服务：车次/座位/排班/余票/订单核心域 |
| train-ticketing-common | - | 公共模块：统一返回、异常处理、日志 AOP |
| web | 9000 | 前端（Vue 3 + Ant Design Vue） |

## 快速开始

1. 依赖：JDK 21、Maven 3.9+、MySQL 8、Redis 7（前端需要 Node 16+；或直接用 Docker）。
2. 初始化数据库：执行 `script/sql/train-ticketing.sql`，自动建库 `train_ticketing` 并创建全部表。
3. 修改 `train-ticketing-member` / `train-ticketing-business` 的 `application.properties` 中数据库、Redis 账号密码。
4. 启动后端（方式一：本地 Maven）：
   ```bash
   # 业务服务（8002，依赖 MySQL + Redis）
   mvn spring-boot:run -pl train-ticketing-business
   # 会员服务（8001）
   mvn spring-boot:run -pl train-ticketing-member
   # 网关（8000）
   mvn spring-boot:run -pl train-ticketing-gateway
   ```
   启动后端（方式二：Docker Compose，中间件 + 后端三服务一键起，账号密码默认对齐无需改配置）：
   ```bash
   mvn -DskipTests package       # 先打包各模块 jar
   docker compose up -d --build  # MySQL(宿主机13306)/Redis(6379)/gateway(8000)/member(8001)/business(8002)
   ```
   > MySQL 容器映射独立宿主机端口 13306（容器内仍 3306），与本机自装 MySQL 的 3306 互不冲突；
   > `member`/`business` 的 `application.properties` 本地开发连接串已指向 `localhost:13306`。
5. 启动前端（npm 或 yarn 均可，也可直接运行一键脚本）：
   ```bash
   cd web
   # 方式一：npm
   npm install && npm run dev
   # 方式二：yarn
   yarn && yarn serve
   # 方式三：一键脚本（Windows 双击 web/dev.bat；Git Bash 执行 web/dev.sh）
   ./dev.sh
   ```
6. 验证：`curl http://localhost:8000/member/member/count`

接口调试文件见 `http/member-member.http`（IDEA HTTP Client 直接运行）。

## 高并发验证

**Service 层**（Redis Lua 多段预扣 → Redisson 分布式锁 → DB 行锁三道防线的并发正确性）：

```bash
docker compose up -d mysql redis   # 只起中间件即可，测试直调 Service 层
mvn test -pl train-ticketing-business -Dtest=OrderConcurrencyTest
```

三个用例：单线程冒烟 → 100 并发抢 10 票（成票数必须精确等于库存）→ A-C/A-B/B-C 跨区间并发（相邻段占用不超库存、缓存与 DB 终态一致）。注意对账定时任务每小时整点运行，测试尽量避开整点。

**HTTP 层压测**（经网关 + JWT 打容器里的真实服务，`script/load/order-load-test.py`）：

```bash
docker compose up -d               # 需要全套服务在跑
python script/load/order-load-test.py                     # 默认 50 库存 / 100 并发 / 200 请求
python script/load/order-load-test.py --stock 20 --concurrency 200 --total 500
```

脚本自动造车次数据链后并发下单，输出成功/余票不足/锁忙分布、延迟分位（p50/p90/p99）、吞吐，并断言防超卖不变式（成功数 ≤ 库存 且 终态余票 = 库存 − 成功数）。

## 可视化监控

```bash
docker compose --profile monitoring up -d
```

Prometheus(9090，抓 cAdvisor + member/business actuator 指标) + Grafana(3000，admin/admin，面板已预置) + cAdvisor(8888)。`TrainTicketing 压测总览` 面板含：服务 CPU/内存、JVM 堆/线程/GC、HikariCP 连接池、HTTP QPS 与 p95/p99 延迟。压测时开着面板即可实时观察。不需要监控时普通 `docker compose up -d` 不会启动这三件套。

## 分支规范

- `master`：受保护主干，只接受 PR 合入，禁止直接推送。
- `feat/*`：功能迭代分支（如 `feat/order-service`、`feat/redis-ticket-cache`）。
- `fix/*`：缺陷修复分支（如 `fix/member-login-bug`）。
- 流程：从 `master` 迁出 `feat/*` → 开发自测 → PR 合回 `master`。

## 数据库设计

见 `script/sql/train-ticketing.sql`（13 张表，全量注释）。

核心是**区间占用余票模型**：12306 的余票不是「车次总票数 - 已售」，而是一张 A→C 的票同时占用 A-B、B-C 两个区间，某区间余票 = 该区间内未被售出的座位数。`train_order_item` 通过 `depart_index / arrive_index` 记录每个座位的占用区间，是后续防超卖与座位分配算法的基础。

## 文档

- [架构说明](docs/architecture.md)
- [数据库设计](docs/database.md)
- [开发规范](AGENTS.md)

## License

[Apache-2.0](LICENSE)

## 参考与致谢

- [nageoffer/12306](https://github.com/nageoffer/12306)：同题材高并发学习项目（Apache-2.0），架构设计思路参考来源。
