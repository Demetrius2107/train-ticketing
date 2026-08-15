# 数据库设计

> 全量 DDL 见 `script/sql/train-ticketing.sql`（唯一入口，建库 `train_ticketing` + 全部表）。

## 1. 全局约定

| 约定 | 说明 |
|---|---|
| 主键 | 雪花算法 `bigint`，代码侧 `IdUtil.getSnowflakeNextId()` 生成 |
| 时间 | `datetime(3)` 毫秒精度；`create_time` 默认 `current_timestamp(3)`，`update_time` 加 `ON UPDATE` |
| 存储 | InnoDB + utf8mb4 |
| 枚举 | `char(1)`，注释注明枚举类名与取值（如 `旅客类型|枚举[PassengerTypeEnum]: 1成人 2儿童 3学生`） |
| 索引 | 唯一键 `uk_*`、普通索引 `idx_*`；手机号/车次编号/订单号等业务唯一键用唯一索引 |
| 表名 | 避免 MySQL 保留字（订单用 `train_order` 而非 `order`） |

## 2. 表清单（13 张）

### 会员模块
| 表 | 说明 | 关键字段/索引 |
|---|---|---|
| member | 会员 | `mobile` 唯一 |
| passenger | 乘车人 | `member_id` 索引 |
| sms_code | 短信验证码 | `mobile + create_time` 索引；`expired_at`、`used_flag` |

### 车次基础数据模块
| 表 | 说明 | 关键字段/索引 |
|---|---|---|
| station | 车站 | `name` 唯一 |
| train | 车次 | `code` 唯一（G1234）；`type` 枚举 |
| train_station | 车次经停站 | 唯一 `(train_id, station_index)`、`(train_id, station_id)` |
| train_carriage | 车厢 | 唯一 `(train_id, carriage_index)`；`seat_type`/`seat_count` |
| train_seat | 座位档案 | 唯一 `(train_id, carriage_id, seat_index, seat_label)` |
| train_price | 票价 | 唯一 `(train_id, seat_type)`；`decimal(10,2)` |

### 每日排班与余票模块
| 表 | 说明 | 关键字段/索引 |
|---|---|---|
| daily_train | 每日车次排班 | 唯一 `(train_id, run_date)`；`status` 停运/运行 |
| daily_train_seat | 当日座位售卖状态 | 唯一 `(daily_train_id, train_seat_id)`；`sale_status` 0可售/1已售/2锁定 |

### 订单模块
| 表 | 说明 | 关键字段/索引 |
|---|---|---|
| train_order | 订单 | `order_no` 唯一；`status` 0待支付/1已支付/2已取消/3已退票；`expire_time` 支付过期 |
| train_order_item | 订单明细 | `order_id` 索引；**`depart_index`/`arrive_index` 记录座位占用区间** |

## 3. 核心：区间占用余票模型

12306 的余票**不是**「车次总票数 − 已售」，而是**区间占用**：

- 一趟车 A→B→C→D，一张 **A→C** 的票会同时占用 **A-B** 和 **B-C** 两个区间段；
- 某区间的余票 = 该区间内**未被售出**的座位数；
- 因此 B-C 段的余票受所有「起点 ≤ B 且终点 ≥ C」的已售车票影响。

落到表结构：

- `daily_train_seat` 记录每个座位当日的售卖状态（0 可售 / 1 已售 / 2 锁定）；
- `train_order_item.depart_index / arrive_index` 记录这张票占用的站序区间；
- 余票查询 = 对目标区间（起点站序 s，终点站序 e）统计 `daily_train_seat` 中可售且未被区间重叠订单占用的座位数。

阶段 1 先以该模型做**可正确防超卖的朴素实现**（数据库行锁/乐观锁）；阶段 2 再叠加 Redis 缓存余票 + Lua 预扣提升吞吐。

## 4. 设计示例：购票落库

用户购买 A→C 二等座 1 张：

1. 校验 `daily_train_seat` 可售（未被 A-C 区间内任何订单占用）；
2. 锁定座位（`sale_status` → 2 锁定）；
3. 创建 `train_order`（`status=0` 待支付，`expire_time=now+10min`）；
4. 创建 `train_order_item`（`depart_index=1, arrive_index=3` 记录占用区间）；
5. 支付成功 → `sale_status=1`、订单 `status=1`；超时未支付 → 释放座位、订单 `status=2`。
