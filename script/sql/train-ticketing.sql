-- ============================================================
-- TrainTicketing 火车票购票系统 数据库脚本
-- 说明：
--   1. 统一约定：id 采用雪花算法 bigint 主键；所有表带 create_time/update_time
--      （datetime(3) 毫秒精度）；存储引擎 InnoDB；字符集 utf8mb4。
--   2. 余票模型（核心）：12306 的余票不是"车次总票数减已售"，而是【区间占用】。
--      一张 A->C 的票同时占用 A-B、B-C 两个区间，某区间余票 = 该区间内未被售出
--      的座位数。train_order_item 记录 depart_index/arrive_index 定位占用区间。
--   3. 枚举字段统一用 char(1)，取值在注释中声明，对应代码中的枚举类。
-- ============================================================

create database if not exists `train_ticketing` default character set utf8mb4 collate utf8mb4_general_ci;
use `train_ticketing`;

-- ------------------------------------------------------------
-- 会员模块
-- ------------------------------------------------------------

-- 会员
drop table if exists `member`;
create table `member`
(
    `id`          bigint      not null comment 'id',
    `mobile`      varchar(11) not null comment '手机号',
    `create_time` datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time` datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_mobile` (`mobile`)
) engine = innodb
  default charset = utf8mb4 comment ='会员';

-- 乘车人
drop table if exists `passenger`;
create table `passenger`
(
    `id`          bigint      not null comment 'id',
    `member_id`   bigint      not null comment '会员id',
    `name`        varchar(20) not null comment '姓名',
    `id_card`     varchar(18) not null comment '身份证号',
    `type`        char(1)     not null comment '旅客类型|枚举[PassengerTypeEnum]: 1成人 2儿童 3学生',
    `create_time` datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time` datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    key `idx_member_id` (`member_id`)
) engine = innodb
  default charset = utf8mb4 comment ='乘车人';

-- 短信验证码
drop table if exists `sms_code`;
create table `sms_code`
(
    `id`          bigint      not null comment 'id',
    `mobile`      varchar(11) not null comment '手机号',
    `code`        varchar(6)  not null comment '验证码',
    `type`        varchar(32) not null comment '业务类型: REGISTER 注册 / LOGIN 登录',
    `expired_at`  datetime(3) not null comment '过期时间',
    `used_flag`   char(1)     not null default '0' comment '是否已使用: 0未使用 1已使用',
    `create_time` datetime(3) not null default current_timestamp(3) comment '发送时间',
    `use_time`    datetime(3) default null comment '使用时间',
    primary key (`id`),
    key `idx_mobile_create_time` (`mobile`, `create_time`)
) engine = innodb
  default charset = utf8mb4 comment ='短信验证码';

-- ------------------------------------------------------------
-- 车次基础数据模块
-- ------------------------------------------------------------

-- 车站
drop table if exists `station`;
create table `station`
(
    `id`           bigint       not null comment 'id',
    `name`         varchar(50)  not null comment '车站名称',
    `name_pinyin`  varchar(100) not null comment '车站拼音全拼',
    `name_py`      varchar(50)  not null comment '车站拼音简拼',
    `city`         varchar(50)  default null comment '所属城市',
    `create_time`  datetime(3)  not null default current_timestamp(3) comment '新增时间',
    `update_time`  datetime(3)  not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_name` (`name`),
    key `idx_city` (`city`)
) engine = innodb
  default charset = utf8mb4 comment ='车站';

-- 车次
drop table if exists `train`;
create table `train`
(
    `id`              bigint      not null comment 'id',
    `code`            varchar(10) not null comment '车次编号，如 G1234',
    `type`            char(1)     not null comment '车次类型|枚举[TrainTypeEnum]: 1高铁 2动车 3特快 4普快',
    `start_station_id` bigint     not null comment '始发站id',
    `end_station_id`  bigint      not null comment '终到站id',
    `start_time`      time        not null comment '始发站发车时间',
    `end_time`        time        not null comment '终到站到达时间',
    `create_time`     datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time`     datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_code` (`code`),
    key `idx_start_station_id` (`start_station_id`),
    key `idx_end_station_id` (`end_station_id`)
) engine = innodb
  default charset = utf8mb4 comment ='车次';

-- 车次经停站
drop table if exists `train_station`;
create table `train_station`
(
    `id`            bigint     not null comment 'id',
    `train_id`      bigint     not null comment '车次id',
    `station_id`    bigint     not null comment '车站id',
    `station_index` int        not null comment '站序，从1开始',
    `arrive_time`   time       default null comment '到达时间（始发站为空）',
    `leave_time`    time       default null comment '发车时间（终到站为空）',
    `stop_minutes`  int        default null comment '停靠分钟数',
    `create_time`   datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time`   datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_train_index` (`train_id`, `station_index`),
    unique key `uk_train_station` (`train_id`, `station_id`)
) engine = innodb
  default charset = utf8mb4 comment ='车次经停站';

-- 车厢
drop table if exists `train_carriage`;
create table `train_carriage`
(
    `id`             bigint     not null comment 'id',
    `train_id`       bigint     not null comment '车次id',
    `carriage_index` int        not null comment '车厢号，从1开始',
    `seat_type`      char(1)    not null comment '座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧',
    `seat_count`     int        not null comment '座位数',
    `create_time`    datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time`    datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_train_carriage_index` (`train_id`, `carriage_index`)
) engine = innodb
  default charset = utf8mb4 comment ='车厢';

-- 座位（车次级座位档案）
drop table if exists `train_seat`;
create table `train_seat`
(
    `id`            bigint     not null comment 'id',
    `train_id`      bigint     not null comment '车次id',
    `carriage_id`   bigint     not null comment '车厢id',
    `seat_index`    int        not null comment '排号',
    `seat_label`    char(1)    not null comment '座位字母: A B C D E F',
    `seat_type`     char(1)    not null comment '座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧',
    `create_time`   datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time`   datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_train_carriage_seat` (`train_id`, `carriage_id`, `seat_index`, `seat_label`),
    key `idx_carriage_id` (`carriage_id`)
) engine = innodb
  default charset = utf8mb4 comment ='座位';

-- 票价（按车次 + 座位类型定价）
drop table if exists `train_price`;
create table `train_price`
(
    `id`          bigint        not null comment 'id',
    `train_id`    bigint        not null comment '车次id',
    `seat_type`   char(1)       not null comment '座位类型|枚举[SeatTypeEnum]',
    `price`       decimal(10, 2) not null comment '票价（元）',
    `create_time` datetime(3)   not null default current_timestamp(3) comment '新增时间',
    `update_time` datetime(3)   not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_train_seat_type` (`train_id`, `seat_type`)
) engine = innodb
  default charset = utf8mb4 comment ='票价';

-- ------------------------------------------------------------
-- 每日排班与余票模块
-- ------------------------------------------------------------

-- 每日车次（按日期排班）
drop table if exists `daily_train`;
create table `daily_train`
(
    `id`              bigint      not null comment 'id',
    `train_id`        bigint      not null comment '车次id',
    `run_date`        date        not null comment '运行日期',
    `start_station_id` bigint     not null comment '始发站id',
    `end_station_id`  bigint      not null comment '终到站id',
    `start_time`      time        not null comment '始发站发车时间',
    `end_time`        time        not null comment '终到站到达时间',
    `status`          char(1)     not null default '1' comment '状态: 0停运 1运行',
    `create_time`     datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time`     datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_train_date` (`train_id`, `run_date`),
    key `idx_run_date` (`run_date`)
) engine = innodb
  default charset = utf8mb4 comment ='每日车次';

-- 当日座位（余票/售卖状态）
drop table if exists `daily_train_seat`;
create table `daily_train_seat`
(
    `id`             bigint     not null comment 'id',
    `daily_train_id` bigint     not null comment '每日车次id',
    `train_seat_id`  bigint     not null comment '座位id',
    `carriage_id`    bigint     not null comment '车厢id',
    `seat_index`     int        not null comment '排号',
    `seat_label`     char(1)    not null comment '座位字母',
    `seat_type`      char(1)    not null comment '座位类型|枚举[SeatTypeEnum]',
    `sale_status`    char(1)    not null default '0' comment '售卖状态: 0可售 1已售 2锁定',
    `create_time`    datetime(3) not null default current_timestamp(3) comment '新增时间',
    `update_time`    datetime(3) not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_daily_train_seat` (`daily_train_id`, `train_seat_id`),
    key `idx_carriage_id` (`carriage_id`)
) engine = innodb
  default charset = utf8mb4 comment ='当日座位';

-- ------------------------------------------------------------
-- 订单模块
-- ------------------------------------------------------------

-- 订单（train_order 避免与 MySQL 保留字 order 冲突）
drop table if exists `train_order`;
create table `train_order`
(
    `id`               bigint        not null comment 'id',
    `order_no`         varchar(32)   not null comment '订单号',
    `member_id`        bigint        not null comment '会员id',
    `daily_train_id`   bigint        not null comment '每日车次id',
    `train_id`         bigint        not null comment '车次id',
    `depart_station_id` bigint       not null comment '出发站id',
    `arrive_station_id` bigint       not null comment '到达站id',
    `run_date`         date          not null comment '乘车日期',
    `status`           char(1)       not null default '0' comment '状态|枚举[OrderStatusEnum]: 0待支付 1已支付 2已取消 3已退票',
    `total_amount`     decimal(10, 2) not null default 0.00 comment '订单总金额（元）',
    `expire_time`      datetime(3)   default null comment '支付过期时间（下单后10分钟）',
    `pay_time`         datetime(3)   default null comment '支付时间',
    `refund_time`      datetime(3)   default null comment '退款时间',
    `create_time`      datetime(3)   not null default current_timestamp(3) comment '下单时间',
    `update_time`      datetime(3)   not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    unique key `uk_order_no` (`order_no`),
    key `idx_member_id` (`member_id`),
    key `idx_status` (`status`),
    key `idx_daily_train_id` (`daily_train_id`)
) engine = innodb
  default charset = utf8mb4 comment ='订单';

-- 订单明细（一个乘车人一张票，含区间占位信息）
drop table if exists `train_order_item`;
create table `train_order_item`
(
    `id`             bigint        not null comment 'id',
    `order_id`       bigint        not null comment '订单id',
    `passenger_id`   bigint        not null comment '乘车人id',
    `passenger_name` varchar(20)   not null comment '乘车人姓名（下单时快照）',
    `id_card`        varchar(18)   not null comment '身份证号（下单时快照）',
    `daily_train_seat_id` bigint   not null comment '当日座位id',
    `seat_type`      char(1)       not null comment '座位类型|枚举[SeatTypeEnum]',
    `price`          decimal(10, 2) not null comment '票价（元）',
    `depart_index`   int           not null comment '出发站序（区间占用起点）',
    `arrive_index`   int           not null comment '到达站序（区间占用终点）',
    `create_time`    datetime(3)   not null default current_timestamp(3) comment '新增时间',
    `update_time`    datetime(3)   not null default current_timestamp(3) on update current_timestamp(3) comment '修改时间',
    primary key (`id`),
    key `idx_order_id` (`order_id`),
    key `idx_passenger_id` (`passenger_id`),
    key `idx_seat_id` (`daily_train_seat_id`)
) engine = innodb
  default charset = utf8mb4 comment ='订单明细';
