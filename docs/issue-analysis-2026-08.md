# 代码问题分析与演进规划（2026-08 审查）

> 审查范围：master 分支截至 `cff2a28`，重点覆盖 business 模块订单/余票/缓存链路、
> common 日志切面、member 登录、gateway 路由、DDL 全量。
> 本文是 `fix/ticket-overbooking-core` 分支的工作依据，按严重程度分级，并给出修复优先级与后续 roadmap。

## 一、严重问题（影响正确性，必须修复）

### 1. Lua 回补脚本硬编码为 +1，回补数量被丢弃（必现 bug）

`TicketCacheService.INCR_SCRIPT`：

```lua
redis.call('HINCRBY', KEYS[1], ARGV[1], 1)   -- 硬编码 1
```

而 `incrRemaining(...)` 把 `count` 作为 `ARGV[2]` 传入，脚本根本没用它。后果：
取消一个 N 人订单，Redis 余票只回补 1，DB 却释放了 N 个座位 → **Redis 余票永久性偏少（少 N−1），
越用越卖不出票**。与 `DECR_SCRIPT`（正确使用了 `ARGV[2]`）明显不对称，属遗漏。

**修法**：`HINCRBY KEYS[1] ARGV[1] tonumber(ARGV[2])`。

### 2. Redis 预扣/回补与 DB 事务不在同一原子域（缓存-DB 不一致）

`OrderService.save()` 是 `@Transactional`，但 Redis `decrRemaining` 在事务内、不归 Spring 事务管。出问题路径：

- Redis 预扣成功 → DB 写 `train_order`/`train_order_item` 抛异常 → DB 回滚，**Redis 已扣不回滚** → 余票凭空减少。
- `cancel`/`expirePendingOrders` 里 `releaseRemaining`（incr）在事务内，事务回滚 → Redis 已回补、DB 没释放 → **余票虚高 → 超卖**。

阶段2号称"防超卖"，实际只降低概率，反而引入不一致窗口。

**修法**：事务提交后再操作缓存（`TransactionSynchronization.afterCommit`）+ 对账兜底。

### 3. Redis 余票缓存的"区间模型"本身是错的（超卖根因）

这是阶段2最致命的设计缺陷。DB 模型是对的（`selectRemainingByInterval` 用 `oi.depart_index <= arrive AND oi.arrive_index >= depart` 判区间重叠），但 Redis 缓存模型对不上：

- 缓存 `field = departIndex_arriveIndex`，把**每个用户请求区间当成独立库存池**。`decrRemaining(1,3,need)` 只扣 `1_3` 这一个 field。
- 一张 A→C 的票实际占用 A-B、B-C 两个相邻子区间。正确模型应按**相邻子区间**（站序 i→i+1）建 field，一张票扣减路径上所有相邻子段；查询 `[d,a]` 余票 = `min(路径上各相邻子段余票)`。
- `initRemainingCache` 把所有区间组合初始化为"座位总数"也是错的：A→C 余票 ≠ 座位总数（A-B 已售时 A→C 也应减）。
- 后果：甲买 A→C（扣 `1_3`），乙查 B→C（`2_3` 仍是满的）→ 两人都下单成功 → **同一座位同一区间卖给两人 = 超卖**。Redis 层完全没挡住，DB 层也没挡住（见问题 4）。

**修法**：Redis 缓存改为相邻子段模型，下单按路径多段原子扣减。

### 4. DB 选座无锁、无唯一约束，并发必超卖

`selectAvailableByInterval` 没有 `FOR UPDATE`，没有 Redisson 分布式锁（阶段2规划了但没落地），`daily_train_seat` 也没有防重唯一索引。两个并发请求查到同一批可售座位并分配同一座位。Redis 预扣本应挡在前面，因问题 3 挡不住跨区间重叠。

**修法**：引入 Redisson 分布式锁（锁粒度 `dailyTrainId:seatType`）+ DB 兜底（选座 `FOR UPDATE` 或唯一约束）。

### 5. `sale_status` 字段形同虚设

DDL 设计了 `0可售/1已售/2锁定`，但下单从不更新它，永远 `'0'`；可售判断全靠 `train_order_item` 实时 LEFT JOIN。取消靠 `deleteByOrderId` 删明细。order_item 既是订单明细又兼做占用记录，职责耦合，数据量上来后余票查询会慢。

## 二、安全/鉴权问题

### 6. 全站无鉴权，订单接口裸奔

`OrderController` 的 `list?memberId=`、`detail?orderNo=`、`cancel`、`pay` 全部无鉴权，任何人可查/取消/支付他人订单；`pay` 的 `memberId` 还是 `required=false`。member 登录返回的 `MemberLoginResp` 无 token，登录后无会话；Gateway 无任何鉴权 filter。

### 7. 敏感信息进日志（违反 AGENTS.md 第5条）

`LogAspect` 里 `excludeProperties = {}` 是空的，注释说"排除身份证/手机号"但实际没配。`OrderSaveReq`、`OrderQueryResp.OrderItemResp` 都含 `idCard`，会被完整打印。

## 三、工程/规范问题

| # | 问题 | 位置 |
|---|---|---|
| 8 | member 模块 req 类放在顶层 `com.trainticketing.req`，business 放 `com.trainticketing.business.req`，包层级不一致 | member/req |
| 9 | 票价按"车次+座位类型"单一价，不分区间距离，A→C 与 A→B 同价 | `train_price` / `OrderService` |
| 10 | 退款流程缺失：`OrderStatusEnum` 有"3已退票"但 `OrderService` 无退款方法，已支付订单无法退 | OrderService |
| 11 | N+1 查询：`queryByStations` 每车次 4 次 DB 查询且余票走实时 JOIN 不走缓存；`queryByMemberId` 每订单查一次 items | TicketService/OrderService |
| 12 | `expirePendingOrders` 全量查过期订单逐条处理、无分页、无 MQ 延时关单（阶段2规划了没做） | OrderService |
| 13 | 时区硬编码不一致：`expireTime` 用 `ZoneOffset.ofHours(8)`，`runDate` 用 `systemDefault()` | OrderService |
| 14 | `genOrderNo` 用 `new SimpleDateFormat`（非线程安全，虽每次 new 不出错但低效），雪花后10位有同毫秒碰撞风险 | OrderService |
| 15 | `DailyTrainService.save` 不自动生成当日座位，需手动调 `generate`，易遗漏导致排班后无余票 | DailyTrainService |
| 16 | 零测试，验证只靠 `mvn compile` | 全局 |

## 四、业务复杂度评估

> 用户的关切：这个项目是否只是 CRUD？业务复杂度是否足够？

按行数算当前约 70% 是 CRUD；但业务领域本身藏着 3-4 个真正硬的非 CRUD 问题，当前实现把它们全部绕过去了：

- **座位分配 = `LIMIT N`**，没有区间图着色/贪心选座算法。真实 12306 核心难点恰是：一张 A→C 卖出后，这个座位还能不能再卖 B→D？当前完全没碰。
- **`sale_status` 永远 '0'**，把可售判断外包给 SQL，业务层无座位状态概念。
- **缓存模型是错的**，阶段2核心"防超卖"名不副实。
- 车站/车次/车厢/座位/会员/乘车人管理纯 CRUD，无业务规则深度。

12306 类项目真正的业务深度在这四个点：
1. 区间占用模型 + 座位分配算法（业务侧唯一硬骨头，当前做错了）
2. 高并发防超卖闭环（缓存-DB 一致性、分布式锁粒度、对账兜底、压测证明）
3. 排队/令牌桶削峰（春运核心，未涉及）
4. 分布式事务（微服务化后订单跨 ticket/order/pay，目前被单体事务掩盖）

参考项目 `nageoffer/12306` 把这四点全做了。本项目当前做到约 30%，且最核心的座位分配空心病。**项目不会"只是 CRUD"，前提是把阶段 2 的硬问题真正做对而不是再做一层样子货。**

## 五、修复优先级与 Roadmap

### 阶段 2.1 — 修正高并发模型（当前分支 `fix/ticket-overbooking-core`，止血）

1. **修 INCR_SCRIPT 的 count bug**（一行改动，立竿见影）
2. **重设 Redis 余票缓存为相邻子区间模型**：
   - field 改为 `i_{i+1}`（相邻站序段）
   - 下单按 `[depart, arrive)` 路径循环扣减所有相邻子段，任一段不足即整体失败（Lua 多段原子扣减）
   - 查询余票 = `min(路径上各相邻子段)`
   - `initRemainingCache` 只对相邻子段初始化为座位总数
3. **缓存-DB 一致性**：事务提交后再扣/回补缓存（`afterCommit`）；定时对账兜底
4. **LogAspect 补敏感字段排除**

### 阶段 2.2 — 补齐订单链路闭环

5. 鉴权：登录签发 JWT，Gateway 全局 filter 校验，OrderController 从 header 取 memberId
6. 退款流程
7. MQ 延时关单替掉全量扫描
8. 票价按区间计价

### 阶段 3 — 微服务化（README 既定方向）

9. user/ticket/order/pay 拆服务 + Nacos + Sentinel
10. ShardingSphere 按 `daily_train_id` 分库分表 `train_order_item`
11. 单元测试 + 并发压测（JMeter）证明防超卖
