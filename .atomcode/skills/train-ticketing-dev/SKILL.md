---
name: train-ticketing-dev
description: TrainTicketing 项目开发流程：新增领域模块（DDL→generator→分层代码→编译验证）、扩展错误码/枚举、遵守项目规范开发时使用。当需要在本项目添加车站/车次/订单等新业务模块或开发新功能时使用。
---

# TrainTicketing 开发流程

本项目规范完整版见 `AGENTS.md`，动手前先读。

## 1. 新增领域模块标准流程（如 station/train/order）

1. **先 DDL**：在 `script/sql/train-ticketing.sql` 末尾追加表定义（雪花 bigint 主键、`datetime(3)` 时间戳、InnoDB + utf8mb4、枚举 `char(1)` 带注释、`uk_`/`idx_` 索引命名）。
2. **生成代码**：在 `train-ticketing-generator/src/main/resources/generator-config-member.xml` 的 `<context>` 里加 `<table tableName="xxx" domainObjectName="Xxx"/>`，运行 MyBatis Generator 生成 domain/mapper（生成文件勿手改）。
3. **分层编码**：
   - `req/`：入参校验类（`@NotBlank(message = "[手机号]不能为空")` 风格）
   - `service/`：业务逻辑，ID 用 `IdUtil.getSnowflakeNextId()`，唯一性冲突抛 `BusinessException`
   - `controller/`：`@RestController` + `@Valid`，返回 `CommonResp<T>`
4. **新错误**：先加 `BusinessExceptionEnum`（中文 desc），再在业务代码使用。
5. **验证**：`mvn -q -DskipTests compile` 通过才算完成。

## 2. 关键约定速查

- 包根 `com.trainticketing`，模块 artifactId `train-ticketing-<模块名>`
- **余票模型是区间占用**（A→C 票占 A-B、B-C 段），不是总数减已售；`train_order_item.depart_index/arrive_index` 记占用区间
- 表名避免 MySQL 保留字（订单用 `train_order` 而非 `order`）
- 数据库名 `train_ticketing`，本地 root/123456，脚本 `script/sql/train-ticketing.sql`
- 网关 8000 → member 8001（context-path `/member`），前端 web 9000

## 3. 常见坑

- 不要手改 generator 生成的 domain/mapper（下次生成会覆盖）
- 敏感字段（手机号/身份证）注意日志排除，不要打进日志
- 改动前确认 `git status`，提交不带 `Co-Authored-By` 尾注
