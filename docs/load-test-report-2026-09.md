# 压测报告与问题修复沉淀（2026-09）

> 本文档沉淀 2026-09 初的两个压测会话：测试方法、发现的问题与修复、系统容量画像、
> 优化路线。所有结论均有可复现实验与监控数据支撑。
> 工具入口：`script/load/`（压测脚本）、`compose --profile monitoring`（Grafana 面板）。

## 一、测试环境与工具

- 部署形态：docker compose 单机全套（gateway 8000 / member 8001 / business 8002 / MySQL 8 / Redis 7）
- 监控：Prometheus(5s 采样) + Grafana「压测总览」面板（CPU/JVM/HikariCP/HTTP QPS/p95p99）+ cAdvisor
- 压测客户端：Python 标准库脚本（单机，注意高并发时客户端本身会成为瓶颈）
  - `script/load/order-load-test.py` —— 下单：冲击模式（同起点放行）/ 持续模式（`--sustain`）
  - `script/load/query-load-test.py` —— 查询：余票查询 70% + 车次列表 30% 混发
  - 两脚本均自动造数（完整车次数据链），查询脚本经 SQL 直插批量占用（形态对 > 绝对量大）
- Service 层并发正确性：`OrderConcurrencyTest`（IDEA/mvn，直调 OrderService）

## 二、测试结果矩阵（最终轮，索引修复后）

| # | 场景 | 压力 | 结果 | 关键延迟 |
|---|---|---|---|---|
| 1 | 下单·春运形态 | 200 请求/100 并发抢 50 票 | 50 成功精确=库存 PASS | p50=544ms |
| 2 | 下单·持续吞吐 | 50 req/s × 60s，库存 2000 | 2000 成功恰好售罄 PASS；732 余票不足+268 锁忙 | 成功 p50≈3.0s |
| 3 | 查询·正常负载 | 100 QPS × 45s | 0 失败，满速 100 req/s | remaining p50=7ms，list p50=35ms |
| 4 | 查询·压力上限 | 300 QPS × 60s | 0 失败，实际 244 req/s（饱和拐点） | remaining p50=3025ms，list p50=4731ms |

历史极端记录：持续模式速率 bug 意外打出 2500 req/s 风暴，5.4 万请求**零超卖**；
春运形态 1 万请求/1000 并发，50 张票精确售出，98.2% 干净拒绝。

**正确性结论**：全部 6 万+ 请求、各种极端形态下，成票数永远精确等于库存、
Redis 缓存与 DB 终态永远一致、拒绝永远干净。防超卖命题证明完毕。

## 三、容量画像与瓶颈排序（Prometheus 数据支撑）

| 资源 | 压测窗口峰值 | 判定 |
|---|---|---|
| business 进程 CPU | 23.8% | JVM 远未到瓶颈 |
| HikariCP active / pending | **10(=上限) / 189** | 连接池饱和，全场最大瓶颈信号 |
| 宿主机 CPU | 94.3%（business 仅占 24%） | **MySQL 是 CPU 大头** |
| JVM 线程 | 268 | Tomcat 200 线程基本占满 |

1. **MySQL 第一瓶颈**：查询干净容量 ≈ 100~150 QPS（p50<50ms），~250 QPS 为饱和拐点
2. **HikariCP 连接池（默认10）第二瓶颈**：pending 189 = 大量线程耗在等连接
3. **下单成票天花板 ≈ 33 单/秒/车次**：Redisson 锁（车次+座位类型粒度）串行化，
   成功请求 p50≈3s 即锁排队等待
4. JVM / 网关 / member 无压力——扩容先扩 MySQL，扩 JVM 无用

## 四、发现的问题与修复（按发现顺序）

| # | 问题 | 根因 | 修复 |
|---|---|---|---|
| 1 | Service 层并发测试从未运行成功 | 启动类在 config 子包，@SpringBootTest 向上搜索不到 | 显式 classes=BusinessApplication |
| 2 | **DB 行锁选座 SQL 一执行就炸** | JOIN 后字段无别名歧义（id ambiguous） | 新增 Alias_Column_List（s. 前缀）|
| 3 | 所有服务端口/context 被劫持成 8001//common | common 残留原型期 config/application.properties（classpath:/config/ 优先级更高） | 删除残留配置 |
| 4 | 网关容器启动失败 predicates 为空 | Spring List 属性跨配置源不合并，routes[n] 被高优先级源整体顶掉 | application-docker.properties 写全路由 |
| 5 | business 容器崩溃循环（Redisson AUTH 失败） | 空的 spring.data.redis.password= 触发 AUTH | 删除空行 |
| 6 | 前端/HTTP 客户端雪花 ID 精度丢失（2.09e18） | 19 位 Long 超 JS 2^53 | 全局 Long→String 序列化 |
| 7 | 下单必报 [会员id]不能为空 | @Valid 先于登录态回填执行，与设计矛盾 | 移除 memberId 的 @NotNull |
| 8 | **下单后余票查询不减**（50 单后仍显示满票） | 占用 JOIN 带 o.status='1'，待支付订单不算占用 | 重写为 NOT EXISTS train_order_item |
| 9 | 余票 count 虚高（50座×38单=1862） | LEFT JOIN 座位×订单行数膨胀 | 同上，NOT EXISTS 无膨胀 |
| 10 | 售罄时余票查询返回空列表而非 0 | group by 无行可聚 | SUM(CASE WHEN) 保证每类型有行 |
| 11 | 懒加载回填污染缓存（未售罄段被写成 min 少卖） | 整区间 min 统一回填各段 | 逐段查 DB、只回填缺失段 |
| 12 | **100 QPS 查询即全站瘫痪**（p50=30s，/hello 都 13.5s） | train_order_item 无 daily_train_seat_id 索引，每座位 NOT EXISTS 全表扫（单查询千万级行比较）→ 10 连接占死 → Tomcat 200 线程排满 | 加 idx_seat_id，p50 30s→7ms（4000 倍） |
| 13 | 容器压测 I/O 大头 | mapper TRACE 行级日志（一次下单打 2000 行） | docker profile 降为 info |
| 14 | 压测脚本持续模式速率=rate² | 每 interval 提交 rate 个请求 | 每 interval 提交 1 个+绝对节拍 |

方法论沉淀：**索引是性价比之王**（一行 DDL，读写两条路径同时受益）；
**LEFT JOIN 判存在不如 NOT EXISTS**（无膨胀、语义准）；**压测必须避开整点**
（对账任务干扰终态断言）。

## 五、优化路线（按数据优先级）

| 项 | 预期收益 | 状态 |
|---|---|---|
| 1. HikariCP 连接池扩容（10→20，连接获取 5s 超时快速失败） | 消除 pending 排队 | ✅ 已实施 |
| 2. 余票查询接 Redis 读缓存（数据现成，摘除查询路径的 MySQL 依赖） | 查询容量上一个数量级 | ✅ 已实施 |
| 3. list 接口 N+1 治理（issue #11：p99 184ms 是 remaining 的 6 倍） | 列表查询延迟减半以上 | ⬜ |
| 4. 下单锁粒度优化（按车厢分段锁） | 抬高 33 单/秒天花板 | ⬜ |

### 优化后复测（2026-09-06，实测后回填）

| 场景 | 优化前 | 优化后 |
|---|---|---|
| 查询 100 QPS | remaining p50=7ms | 待复测 |
| 查询 300 QPS | 实际 244 req/s，remaining p50=3025ms，pending 峰值 189 | 待复测 |
| 下单持续 50rps | 成功 p50≈3.0s（锁排队为主） | 待复测 |

## 六、复现命令

```bash
# 环境
docker compose --profile monitoring up -d   # 全套 + 监控（Grafana:3000 admin/admin）

# Service 层并发正确性（避开整点）
mvn clean test -pl train-ticketing-business -am -Dtest=OrderConcurrencyTest

# HTTP 压测（四轮见第二节）
python script/load/order-load-test.py                                        # 春运冲击
python script/load/order-load-test.py --sustain 60 --rate 50 --stock 2000    # 下单持续
python script/load/query-load-test.py --duration 60 --rate 100               # 查询正常
python script/load/query-load-test.py --duration 60 --rate 300               # 查询上限

# Grafana：Last 15m + 5s 刷新，看 QPS/连接池 pending/CPU/线程 四件套
```
