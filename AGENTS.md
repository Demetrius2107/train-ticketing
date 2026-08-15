# AGENTS.md — TrainTicketing 项目开发规范

> 本文件是 AtomCode 在本仓库工作时的**项目级指令**，优先级高于默认规则。
> 动手前先读本文件、README 与 `docs/`；与本文件冲突的默认规则以本文件为准。

## 1. 项目定位

高仿 12306 火车票购票系统（分布式高并发学习项目）。前身 `12306-train-system`（2024-02 原型工程），2026-08 重启并标准化：重命名、统一模块前缀、升级 JDK21、重写 DDL。

Roadmap（详见 README）：
- 阶段 0 工程地基（统一异常/返回/日志、会员注册登录）— ✅ 基础完成（短信通道为占位实现）
- 阶段 1 单体核心域：车站/车次/车厢/座位/每日排班/区间占用余票/订单 — ⬜ 当前阶段
- 阶段 2 高并发三板斧：Redis 余票缓存 + Lua 预扣、Redisson 分布式锁、MQ 削峰 + 延时关单 — ⬜
- 阶段 3 微服务化 + 分库分表 — ⬜

## 2. 技术栈（版本已在根 pom.xml 验证，勿随意降级/改动）

- JDK 21（本机 `D:\jdk21`；构建用系统 Maven 3.9.12，mvnw 仅作兜底）
- Spring Boot **3.3.13** / Spring Cloud **2023.0.3**（JDK 21 需要 Boot ≥ 3.2）
- Spring Cloud Gateway、MyBatis（Generator 生成）、MySQL 8、Vue 3 + Ant Design Vue

## 3. 模块与命名约定

| 模块 | artifactId | 端口 | 说明 |
|---|---|---|---|
| common | `train-ticketing-common` | - | 统一返回/异常处理/日志 AOP |
| member | `train-ticketing-member` | 8001 | 会员：注册/验证码/登录/乘车人（context-path `/member`） |
| gateway | `train-ticketing-gateway` | 8000 | 网关路由 `/member/**` |
| generator | `train-ticketing-generator` | - | MyBatis Generator |
| web | （Vue 工程） | 9000 | 前端 |

- Java 包根：**`com.trainticketing`**（如 `com.trainticketing.member.service`），新增代码一律用此包根
- 新模块 artifactId 一律 `train-ticketing-<模块名>`，并在根 pom `<modules>` 注册
- 类分层：`Controller → Service → Mapper`；`domain/mapper` 由 Generator 生成，**勿手改生成文件**

## 4. 数据库规范

- 库名 `train_ticketing`；DDL 唯一入口：**`script/sql/train-ticketing.sql`**（新增表先追加 DDL 再动代码）
- 字段约定：雪花 bigint 主键、`create_time/update_time datetime(3)`、InnoDB + utf8mb4、枚举 `char(1)` 且注释注明枚举类名
- **核心模型：余票 = 区间占用**。一张 A→C 的票同时占用 A-B、B-C 区间，某区间余票 = 该区间未售座位数；`train_order_item.depart_index/arrive_index` 记录占用区间。不是"总数减已售"！
- 需要新表代码时：改 `train-ticketing-generator/src/main/resources/generator-config-member.xml` 的 `<table>`，运行 generator 生成

## 5. 编码规范

- 统一返回 `CommonResp<T>`；业务异常 `BusinessException` + `BusinessExceptionEnum`（新错误先加枚举）
- Controller 入参用 req 类 + `@Valid`（校验注解 message 用 `[字段名]不能为空` 风格）
- ID 生成统一 `cn.hutool.core.util.IdUtil.getSnowflakeNextId()`
- 手机号/身份证等敏感字段不进日志（LogAspect 已做参数打印，注意排除配置）

## 6. 验证命令（每次改完代码必须执行，禁止"只改不验证"）

```bash
mvn -q -DskipTests compile
```

- 不要启动 dev server / watcher / 全量打包
- 前端改动：至少校验 `web/package.json` 可解析；有把握时跑 `cd web && npx vue-cli-service build`

## 7. 分支与提交规范

- `master` 为受保护主干，由用户手动合并；日常从 master 迁出 `feat/<功能>`、`fix/<缺陷>` 分支迭代
- commit 遵循 Conventional Commits（`fix/feat/docs/refactor/chore` 前缀，中文正文）
- **commit 信息一律不要出现 atomcode 字样**（不带 `Co-Authored-By: AtomCode` 尾注，主题/正文也不提及；用户明确要求）
- 提交前先 `git status` 确认暂存范围，提交后工作区保持干净

## 8. 文档索引

- `README.md` — 总览 / 快速开始 / Roadmap / 分支规范
- `docs/architecture.md` — 架构与请求链路
- `docs/database.md` — 表设计与余票模型详解
- `script/sql/train-ticketing.sql` — 全量 DDL
