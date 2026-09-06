package com.trainticketing.business.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.trainticketing.business.config.OrderMqConfig;
import com.trainticketing.business.config.TicketLockConfig;
import com.trainticketing.business.domain.DailyTrain;
import com.trainticketing.business.domain.DailyTrainSeat;
import com.trainticketing.business.domain.TrainOrder;
import com.trainticketing.business.domain.TrainOrderItem;
import com.trainticketing.business.domain.TrainPrice;
import com.trainticketing.business.domain.TrainStation;
import com.trainticketing.business.enums.OrderStatusEnum;
import com.trainticketing.business.mapper.DailyTrainMapper;
import com.trainticketing.business.mapper.DailyTrainSeatMapper;
import com.trainticketing.business.mapper.TrainOrderItemMapper;
import com.trainticketing.business.mapper.TrainOrderMapper;
import com.trainticketing.business.mapper.TrainPriceMapper;
import com.trainticketing.business.mapper.TrainStationMapper;
import com.trainticketing.business.message.OrderCreateMessage;
import com.trainticketing.business.req.OrderSaveReq;
import com.trainticketing.business.resp.OrderQueryResp;
import com.trainticketing.business.service.seat.SeatAllocationStrategy;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>Title: OrderService</p>
 * <p>Description: 订单服务：同步下单（校验→锁→预扣→选座→落库）与异步下单（预扣→发消息→消费者出票），
 * 含取消/支付/退票、延时消息关单与兜底扫描</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-09-06
 * @since 1.0
 */
@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    @Resource
    private TrainOrderMapper trainOrderMapper;

    @Resource
    private TrainOrderItemMapper trainOrderItemMapper;

    @Resource
    private DailyTrainMapper dailyTrainMapper;

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private TrainStationMapper trainStationMapper;

    @Resource
    private TrainPriceMapper trainPriceMapper;

    @Resource
    private TicketCacheService ticketCacheService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SeatAllocationStrategy seatAllocationStrategy;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private OrderMqConfig orderMqConfig;

    /**
     * 自身代理引用：下单需先加分布式锁（非事务）再进入事务方法，避免同类自调用导致 @Transactional 失效
     */
    @Lazy
    @Resource
    private OrderService self;

    /**
     * 下单入口：为每个乘车人分配一个区间可售座位，生成订单 + 订单明细。
     * <p>高并发防超卖三道防线：
     * 1. Redisson 分布式锁（粒度 dailyTrainId:seatType）串行化同座位类型下单；
     * 2. Redis Lua 原子预扣区间余票（相邻子段模型）；
     * 3. DB 选座 FOR UPDATE 行锁兜底。
     * <p>锁在事务外层（持锁→进事务→提交/回滚→释放），避免锁释放早于事务提交导致并发穿透。
     * 事务体在 {@link #saveInTx}，通过 self 代理调用以保证 @Transactional 生效。
     *
     * @param req 下单请求
     * @return 订单号
     */
    public String save(OrderSaveReq req) {
        // 幂等拦截：前端传 idempotentKey，Redis SETNX 占位 5 分钟，重复提交直接拒绝
        if (!acquireIdempotent(req.getMemberId(), req.getIdempotentKey())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_IDEMPOTENT_REPEAT);
        }
        // 只读校验放锁外，快速失败
        DailyTrain dailyTrain = dailyTrainMapper.selectById(req.getDailyTrainId());
        if (ObjectUtil.isNull(dailyTrain)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_TRAIN_NOT_EXIST);
        }
        TrainStation depart = trainStationMapper.selectByStation(dailyTrain.getTrainId(), req.getDepartStationId());
        TrainStation arrive = trainStationMapper.selectByStation(dailyTrain.getTrainId(), req.getArriveStationId());
        if (ObjectUtil.isNull(depart) || ObjectUtil.isNull(arrive)
                || depart.getStationIndex() >= arrive.getStationIndex()) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_INDEX_INVALID);
        }
        // 分布式锁：同排班同座位类型串行化，不同座位类型可并行
        String lockKey = TicketLockConfig.lockKey(req.getDailyTrainId(), req.getSeatType());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(TicketLockConfig.LOCK_WAIT_SECONDS, TicketLockConfig.LOCK_LEASE_SECONDS,
                    java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_LOCK_BUSY);
            }
            return self.saveInTx(req, dailyTrain, depart, arrive);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_LOCK_BUSY);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 幂等缓存 key 前缀：order:idem:{memberId}:{idempotentKey}
     */
    private static final String IDEM_KEY_PREFIX = "order:idem:";
    /**
     * 幂等占位有效期：5 分钟，覆盖下单+支付窗口
     */
    private static final java.time.Duration IDEM_TTL = java.time.Duration.ofMinutes(5);

    /**
     * 预扣补偿上下文 key 前缀：order:predecr:{orderId}。
     * 异步下单 Lua 预扣的参数凭据——订单行不含区间站序/座位类型，兜底扫描回补悬挂单时需要
     */
    private static final String PRE_DECR_KEY_PREFIX = "order:predecr:";
    /**
     * 预扣上下文有效期：30 分钟，覆盖出票+支付窗口；超期未消费的由对账任务按 DB 终态修正缓存
     */
    private static final java.time.Duration PRE_DECR_TTL = java.time.Duration.ofMinutes(30);

    /**
     * 幂等占位：SETNX 语义，key 存在返回 false（重复提交），否则占位返回 true。
     * 用 Redisson RBucket.setIfAbsent 实现，与下单链路共用 RedissonClient。
     *
     * @param memberId      会员ID
     * @param idempotentKey 幂等键（前端生成）
     * @return true 首次提交可继续；false 重复提交
     */
    private boolean acquireIdempotent(Long memberId, String idempotentKey) {
        String key = idemKey(memberId, idempotentKey);
        return redissonClient.getBucket(key).setIfAbsent("1", IDEM_TTL);
    }

    /**
     * 构建幂等缓存 key：order:idem:{memberId}:{idempotentKey}
     *
     * @param memberId      会员ID
     * @param idempotentKey 幂等键（前端生成）
     * @return 缓存 key
     */
    private String idemKey(Long memberId, String idempotentKey) {
        return IDEM_KEY_PREFIX + memberId + ":" + idempotentKey;
    }

    /**
     * 下单事务体：Redis 预扣 + DB 选座（行锁兜底）+ 生成订单/明细。
     * 由 {@link #save} 持分布式锁后通过 self 代理调用，保证 @Transactional 代理生效。
     *
     * @param req        下单请求
     * @param dailyTrain 排班（锁外已查）
     * @param depart     出发经停站
     * @param arrive     到达经停站
     * @return 订单号
     */
    @Transactional
    public String saveInTx(OrderSaveReq req, DailyTrain dailyTrain, TrainStation depart, TrainStation arrive) {
        int need = req.getPassengers().size();
        // 1. Redis Lua 原子预扣区间余票（按乘车人数），防并发超卖
        long remainAfter = ticketCacheService.decrRemaining(
                req.getDailyTrainId(), req.getSeatType(), depart.getStationIndex(), arrive.getStationIndex(), need);
        if (remainAfter < 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_NOT_ENOUGH);
        }
        // 缓存-DB 一致性：预扣成功后，若本事务回滚（DB 写入失败/校验异常）则回补已扣缓存，
        // 避免缓存凭空减少。提交成功则保留扣减。
        registerRollbackCompensate(req.getDailyTrainId(), req.getSeatType(),
                depart.getStationIndex(), arrive.getStationIndex(), need);
        // 2. DB 选座（FOR UPDATE 全量加锁）+ 贪心策略分配相邻座位
        List<DailyTrainSeat> availableSeats = dailyTrainSeatMapper.selectAllAvailableForUpdate(
                req.getDailyTrainId(), depart.getStationIndex(), arrive.getStationIndex(),
                req.getSeatType());
        List<DailyTrainSeat> allocated = seatAllocationStrategy.allocate(availableSeats, req.getSeatType(), need);
        if (CollUtil.isEmpty(allocated) || allocated.size() < need) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_NOT_ENOUGH);
        }
        // 票价（按车次+座位类型，单价）
        TrainPrice trainPrice = trainPriceMapper.selectByTrainAndType(dailyTrain.getTrainId(), req.getSeatType());
        if (ObjectUtil.isNull(trainPrice)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_PRICE_NOT_EXIST);
        }
        // 生成订单
        long now = System.currentTimeMillis();
        TrainOrder order = new TrainOrder();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setOrderNo(genOrderNo(now));
        order.setMemberId(req.getMemberId());
        order.setDailyTrainId(req.getDailyTrainId());
        order.setTrainId(dailyTrain.getTrainId());
        order.setDepartStationId(req.getDepartStationId());
        order.setArriveStationId(req.getArriveStationId());
        order.setRunDate(Date.from(req.getRunDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        order.setStatus(OrderStatusEnum.PENDING.getCode());
        order.setTotalAmount(trainPrice.getPrice().multiply(BigDecimal.valueOf(need)));
        order.setExpireTime(Date.from(LocalDateTime.now().plusMinutes(10).toInstant(java.time.ZoneOffset.ofHours(8))));
        order.setCreateTime(new Date(now));
        order.setUpdateTime(new Date(now));
        trainOrderMapper.insert(order);
        // 生成订单明细（一个乘车人一张票，记录区间占位）
        List<TrainOrderItem> items = new ArrayList<>(need);
        for (int i = 0; i < need; i++) {
            DailyTrainSeat seat = allocated.get(i);
            OrderSaveReq.PassengerReq passenger = req.getPassengers().get(i);
            TrainOrderItem item = new TrainOrderItem();
            item.setId(IdUtil.getSnowflakeNextId());
            item.setOrderId(order.getId());
            item.setPassengerId(passenger.getPassengerId());
            item.setPassengerName(passenger.getName());
            item.setIdCard(passenger.getIdCard());
            item.setDailyTrainSeatId(seat.getId());
            item.setSeatType(req.getSeatType());
            item.setPrice(trainPrice.getPrice());
            item.setDepartIndex(depart.getStationIndex());
            item.setArriveIndex(arrive.getStationIndex());
            item.setCreateTime(new Date(now));
            item.setUpdateTime(new Date(now));
                trainOrderItemMapper.insert(item);
            items.add(item);
        }
        // 双轨行为一致：同步下单同样注册延时关单消息（事务提交后发送，失败仅告警由兜底扫描接管）
        registerCloseDelayAfterCommit(order.getId());
        LOG.info("下单成功 orderNo={}, memberId={}, items={}", order.getOrderNo(), req.getMemberId(), items.size());
        return order.getOrderNo();
    }

    /**
     * 取消订单：仅待支付订单可取消；删除明细（释放区间占用）并置状态为已取消（事务）。
     *
     * @param orderNo 订单号
     */
    @Transactional
    public void cancel(String orderNo) {
        TrainOrder order = trainOrderMapper.selectByOrderNo(orderNo);
        if (ObjectUtil.isNull(order)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_NOT_EXIST);
        }
        if (!OrderStatusEnum.PENDING.getCode().equals(order.getStatus())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_STATUS_INVALID);
        }
        // CAS 状态转换：PENDING→CANCELLED，先判状态再删明细，防并发支付竞态。
        // 若已被支付（CAS 失败），不删明细直接报错，避免已支付订单丢明细。
        int updated = trainOrderMapper.updateStatusIfMatch(order.getId(),
                OrderStatusEnum.PENDING.getCode(), OrderStatusEnum.CANCELLED.getCode(), null, null);
        if (updated == 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_CONCURRENT_CONFLICT);
        }
        // 先取回补参数（读明细）再删明细：releaseRemainingAfterCommit 需要在删除前读取区间参数，
        // 注册的 afterCommit 回调在事务提交（明细已删）后执行回补。若先删后读会恒查空、余票永不回补
        //（该 bug 曾被整点对账按 DB 重建缓存掩盖）。
        releaseRemainingAfterCommit(order);
        trainOrderItemMapper.deleteByOrderId(order.getId());
        LOG.info("取消订单成功 orderNo={}, memberId={}", orderNo, order.getMemberId());
    }

    /**
     * 回补订单占用的 Redis 区间余票：按订单明细的区间（departIndex/arriveIndex）、
     * 座位类型与明细数量，将余票加回缓存。
     * <p>调用时机：必须在 {@code deleteByOrderId} 之前调用——方法内部要读明细取回补参数，
     * 删除后读恒为空会导致余票永不回补；注册的 afterCommit 回调在事务提交（明细已删）后才执行回补。
     * <p>缓存-DB 一致性：注册为事务提交后回调，确保 DB 释放成功后才回补缓存，
     * 避免事务回滚时缓存已回补而 DB 未释放导致余票虚高超卖。
     *
     * @param order 订单实体
     */
    private void releaseRemainingAfterCommit(TrainOrder order) {
        List<TrainOrderItem> items = trainOrderItemMapper.selectByOrderId(order.getId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        TrainOrderItem first = items.get(0);
        Long dailyTrainId = order.getDailyTrainId();
        String seatType = first.getSeatType();
        Integer departIndex = first.getDepartIndex();
        Integer arriveIndex = first.getArriveIndex();
        int count = items.size();
        // 已在事务内读取明细，删除发生在本事务；afterCommit 时明细已删，故此处提前取好参数
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ticketCacheService.incrRemaining(dailyTrainId, seatType, departIndex, arriveIndex, count);
                }
            });
        } else {
            ticketCacheService.incrRemaining(dailyTrainId, seatType, departIndex, arriveIndex, count);
        }
    }

    /**
     * 下单预扣后的回滚补偿：若本事务回滚（DB 写入失败/校验异常），
     * 回补已扣的 Redis 余票，避免缓存凭空减少。
     *
     * @param dailyTrainId 排班ID
     * @param seatType     座位类型
     * @param departIndex  出发站序
     * @param arriveIndex  到达站序
     * @param need         已预扣数量
     */
    private void registerRollbackCompensate(Long dailyTrainId, String seatType,
                                            Integer departIndex, Integer arriveIndex, int need) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    ticketCacheService.incrRemaining(dailyTrainId, seatType, departIndex, arriveIndex, need);
                }
            }
        });
    }

    /**
     * 订单支付：仅待支付订单可支付，且未超过支付过期时间；
     * 支付成功后状态置为已支付并记录支付时间（事务）。
     *
     * @param orderNo  订单号
     * @param memberId 支付会员ID
     */
    @Transactional
    public void pay(String orderNo, Long memberId) {
        TrainOrder order = trainOrderMapper.selectByOrderNo(orderNo);
        if (ObjectUtil.isNull(order)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_NOT_EXIST);
        }
        // 仅限订单归属会员支付
        if (ObjectUtil.isNotNull(memberId) && !order.getMemberId().equals(memberId)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_STATUS_INVALID);
        }
        if (!OrderStatusEnum.PENDING.getCode().equals(order.getStatus())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_STATUS_INVALID);
        }
        // 支付过期校验：超过 expire_time 视为超时不可支付
        if (ObjectUtil.isNotNull(order.getExpireTime())
                && order.getExpireTime().before(new Date())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_PAY_EXPIRED);
        }
        // CAS 状态转换：PENDING→PAID，防并发关单竞态（影响行数0说明已被关单）
        int updated = trainOrderMapper.updateStatusIfMatch(order.getId(),
                OrderStatusEnum.PENDING.getCode(), OrderStatusEnum.PAID.getCode(), new Date(), null);
        if (updated == 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_CONCURRENT_CONFLICT);
        }
        LOG.info("订单支付成功 orderNo={}, memberId={}", orderNo, order.getMemberId());
    }

    /**
     * 退票：仅已支付订单可退；删除明细释放区间占用、置状态为已退票并记录退款时间（事务）。
     * 退票后座位重新可售，Redis 余票在事务提交后回补（与取消一致的释放逻辑）。
     * <p>与取消的区别：取消针对待支付订单（未付款），退票针对已支付订单（已付款，需走退款）。
     *
     * @param orderNo  订单号
     * @param memberId 操作会员ID（仅订单归属会员可退）
     */
    @Transactional
    public void refund(String orderNo, Long memberId) {
        TrainOrder order = trainOrderMapper.selectByOrderNo(orderNo);
        if (ObjectUtil.isNull(order)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_NOT_EXIST);
        }
        // 仅限订单归属会员退票
        if (ObjectUtil.isNotNull(memberId) && !order.getMemberId().equals(memberId)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_STATUS_INVALID);
        }
        if (!OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_STATUS_INVALID);
        }
        // CAS 状态转换：PAID→REFUNDED，先判状态再删明细，防重复退票竞态。
        // 若状态已变（CAS 失败），不删明细直接报错，避免丢明细。
        int updated = trainOrderMapper.updateStatusIfMatch(order.getId(),
                OrderStatusEnum.PAID.getCode(), OrderStatusEnum.REFUNDED.getCode(), null, new Date());
        if (updated == 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_CONCURRENT_CONFLICT);
        }
        // 先取回补参数（读明细）再删明细，原因同 cancel：删后读恒为空会导致余票永不回补
        releaseRemainingAfterCommit(order);
        trainOrderItemMapper.deleteByOrderId(order.getId());
        LOG.info("退票成功 orderNo={}, memberId={}", orderNo, order.getMemberId());
    }

    /**
     * 异步下单（MQ 削峰）：请求线程只做幂等、校验、Redis Lua 预扣与发消息，不碰 DB 写路径
     * （仅预插一行"出票中"订单供前端轮询）；选座出票由消费者 {@code OrderCreateConsumer} 异步完成。
     * <p>与同步 {@link #save} 的差别：
     * 1. Lua 预扣仍在请求线程（防超卖第一道防线不变），行锁选座延后到消费端，DB 写入被 MQ 削峰填谷；
     * 2. 立即返回排队订单号，订单状态 4出票中 → 0待支付（成功）/ 5出票失败（余票已回补）。
     * <p>失败补偿闭环：预插订单/发消息失败 → 本方法内 CAS 置失败 + 回补预扣；
     * 消费端余票耗尽 → 消费者置失败 + 回补；消息丢失/重试耗尽 → {@link #sweepTimeoutOrders} 兜底收敛。
     *
     * @param req 下单请求（memberId 已由控制器回填登录态）
     * @return 排队订单号（前端凭此轮询订单状态）
     */
    public String saveAsync(OrderSaveReq req) {
        long now = System.currentTimeMillis();
        long orderId = IdUtil.getSnowflakeNextId();
        String orderNo = genOrderNo(now);
        // 1. 幂等占位：value 存预生成订单号，重复提交直接返回首次单号（前端轮询同一单）。
        // 先读后 SETNX：并发同键请求中，SETNX 落败方回读胜者单号返回，保证同一幂等键对外只有一个单号
        org.redisson.api.RBucket<String> idemBucket = redissonClient.getBucket(idemKey(req.getMemberId(), req.getIdempotentKey()));
        String prev = idemBucket.get();
        if (ObjectUtil.isNull(prev)) {
            if (idemBucket.setIfAbsent(orderNo, IDEM_TTL)) {
                prev = null;
            } else {
                prev = idemBucket.get();
            }
        }
        if (ObjectUtil.isNotNull(prev)) {
            return prev;
        }
        // 2. 只读校验放请求线程快速失败：排班、站序、票价（与同步链路一致）
        DailyTrain dailyTrain = dailyTrainMapper.selectById(req.getDailyTrainId());
        if (ObjectUtil.isNull(dailyTrain)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_DAILY_TRAIN_NOT_EXIST);
        }
        TrainStation depart = trainStationMapper.selectByStation(dailyTrain.getTrainId(), req.getDepartStationId());
        TrainStation arrive = trainStationMapper.selectByStation(dailyTrain.getTrainId(), req.getArriveStationId());
        if (ObjectUtil.isNull(depart) || ObjectUtil.isNull(arrive)
                || depart.getStationIndex() >= arrive.getStationIndex()) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_STATION_INDEX_INVALID);
        }
        TrainPrice trainPrice = trainPriceMapper.selectByTrainAndType(dailyTrain.getTrainId(), req.getSeatType());
        if (ObjectUtil.isNull(trainPrice)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_TRAIN_PRICE_NOT_EXIST);
        }
        int need = req.getPassengers().size();
        int departIndex = depart.getStationIndex();
        int arriveIndex = arrive.getStationIndex();
        // 3. Lua 原子预扣区间余票：削峰后防超卖的第一道防线仍是 Redis 计数器
        if (ticketCacheService.decrRemaining(req.getDailyTrainId(), req.getSeatType(), departIndex, arriveIndex, need) < 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_NOT_ENOUGH);
        }
        boolean orderInserted = false;
        boolean contextSaved = false;
        try {
            // 4. 预插"出票中"订单（单行插入，毫秒级），前端立即可以轮询到
            TrainOrder order = buildQueuingOrder(orderId, orderNo, req, dailyTrain, trainPrice, need, now);
            trainOrderMapper.insert(order);
            orderInserted = true;
            // 5. 记录预扣上下文：兜底扫描回补悬挂单时需要（订单行不含区间站序/座位类型）
            savePreDecrContext(orderId, req.getDailyTrainId(), req.getSeatType(), departIndex, arriveIndex, need);
            contextSaved = true;
            // 6. 同步发送出票消息（生产者可靠性的第一环：失败当场补偿，不留悬挂）
            rocketMQTemplate.syncSend(orderMqConfig.getCreateTopic(),
                    MessageBuilder.withPayload(buildCreateMessage(order, req, trainPrice, departIndex, arriveIndex)).build());
            LOG.info("异步下单受理 orderNo={}, memberId={}, items={}", orderNo, req.getMemberId(), need);
            return orderNo;
        } catch (RuntimeException e) {
            LOG.error("异步下单失败，进入补偿 orderNo={}, orderInserted={}, error={}", orderNo, orderInserted, e.getMessage());
            compensateQueuingOrder(orderId, req.getDailyTrainId(), req.getSeatType(),
                    departIndex, arriveIndex, need, orderInserted, contextSaved);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 构建"出票中"订单行（生产者预插，消费者只补明细）
     *
     * @param orderId    预生成订单ID
     * @param orderNo    预生成订单号
     * @param req        下单请求
     * @param dailyTrain 排班
     * @param trainPrice 票价
     * @param need       乘车人数
     * @param now        当前时间戳
     * @return 订单实体
     */
    private TrainOrder buildQueuingOrder(long orderId, String orderNo, OrderSaveReq req, DailyTrain dailyTrain,
                                         TrainPrice trainPrice, int need, long now) {
        TrainOrder order = new TrainOrder();
        order.setId(orderId);
        order.setOrderNo(orderNo);
        order.setMemberId(req.getMemberId());
        order.setDailyTrainId(req.getDailyTrainId());
        order.setTrainId(dailyTrain.getTrainId());
        order.setDepartStationId(req.getDepartStationId());
        order.setArriveStationId(req.getArriveStationId());
        order.setRunDate(Date.from(req.getRunDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        order.setStatus(OrderStatusEnum.QUEUING.getCode());
        order.setTotalAmount(trainPrice.getPrice().multiply(BigDecimal.valueOf(need)));
        order.setExpireTime(Date.from(LocalDateTime.now().plusMinutes(10).toInstant(java.time.ZoneOffset.ofHours(8))));
        order.setCreateTime(new Date(now));
        order.setUpdateTime(new Date(now));
        return order;
    }

    /**
     * 构建出票消息体：显式携带出票所需全部参数（区间站序/单价/乘车人快照），消费端零回查
     */
    private OrderCreateMessage buildCreateMessage(TrainOrder order, OrderSaveReq req, TrainPrice trainPrice,
                                                  int departIndex, int arriveIndex) {
        OrderCreateMessage message = new OrderCreateMessage();
        message.setOrderId(order.getId());
        message.setOrderNo(order.getOrderNo());
        message.setDailyTrainId(order.getDailyTrainId());
        message.setSeatType(req.getSeatType());
        message.setDepartIndex(departIndex);
        message.setArriveIndex(arriveIndex);
        message.setUnitPrice(trainPrice.getPrice());
        List<OrderCreateMessage.Passenger> passengers = new ArrayList<>(req.getPassengers().size());
        for (OrderSaveReq.PassengerReq passengerReq : req.getPassengers()) {
            OrderCreateMessage.Passenger passenger = new OrderCreateMessage.Passenger();
            passenger.setPassengerId(passengerReq.getPassengerId());
            passenger.setName(passengerReq.getName());
            passenger.setIdCard(passengerReq.getIdCard());
            passengers.add(passenger);
        }
        message.setPassengers(passengers);
        return message;
    }

    /**
     * 保存预扣上下文（order:predecr:{orderId}，30 分钟）：
     * 记录 Lua 预扣参数供兜底扫描回补悬挂单，消费成功/终态失败后清除
     */
    private void savePreDecrContext(Long orderId, Long dailyTrainId, String seatType,
                                    int departIndex, int arriveIndex, int need) {
        JSONObject ctx = new JSONObject();
        ctx.set("dailyTrainId", dailyTrainId);
        ctx.set("seatType", seatType);
        ctx.set("departIndex", departIndex);
        ctx.set("arriveIndex", arriveIndex);
        ctx.set("need", need);
        redissonClient.getBucket(PRE_DECR_KEY_PREFIX + orderId).set(ctx.toString(), PRE_DECR_TTL);
    }

    /**
     * 异步下单请求线程内的失败补偿：出票中订单 CAS 置失败（CAS 成功者回补，防与消费者/兜底双补偿），
     * 订单行未插入时直接回补（无可 CAS 的行）；最后清除预扣上下文
     */
    private void compensateQueuingOrder(Long orderId, Long dailyTrainId, String seatType,
                                        int departIndex, int arriveIndex, int need,
                                        boolean orderInserted, boolean contextSaved) {
        if (orderInserted) {
            failQueuingOrder(orderId, dailyTrainId, seatType, departIndex, arriveIndex, need);
        } else {
            ticketCacheService.incrRemaining(dailyTrainId, seatType, departIndex, arriveIndex, need);
        }
        if (contextSaved) {
            redissonClient.getBucket(PRE_DECR_KEY_PREFIX + orderId).delete();
        }
    }

    /**
     * 消费出票：分布式锁（与同步链路同粒度）串行化选座后进入事务体。
     * 抛出的异常由消费者分型：余票耗尽/并发冲突为终态，锁忙等临时失败走消息重试。
     *
     * @param message 出票消息
     */
    public void processAsyncOrder(OrderCreateMessage message) {
        String lockKey = TicketLockConfig.lockKey(message.getDailyTrainId(), message.getSeatType());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(TicketLockConfig.LOCK_WAIT_SECONDS, TicketLockConfig.LOCK_LEASE_SECONDS,
                    java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_LOCK_BUSY);
            }
            self.processAsyncOrderInTx(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_LOCK_BUSY);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 出票事务体（经 self 代理调用保证 @Transactional 生效）：
     * 1. 消费幂等：明细已存在说明该消息处理成功过（RocketMQ 至少一次投递，重复是常态）；
     * 2. 状态守卫：仅出票中可出票（已被兜底扫描终态化的直接 ACK 放弃）；
     * 3. DB 行锁选座 + 插明细 + CAS 出票中→待支付；
     * 4. 事务提交后发送延时关单消息。
     *
     * @param message 出票消息
     */
    @Transactional
    public void processAsyncOrderInTx(OrderCreateMessage message) {
        // 1. 消费幂等：至少一次投递下，重复消息靠订单明细已存在识别
        List<TrainOrderItem> existing = trainOrderItemMapper.selectByOrderId(message.getOrderId());
        if (CollUtil.isNotEmpty(existing)) {
            LOG.info("出票消息重复投递，已处理过，跳过 orderNo={}", message.getOrderNo());
            return;
        }
        TrainOrder order = trainOrderMapper.selectById(message.getOrderId());
        if (ObjectUtil.isNull(order)) {
            // 理论不可达（订单行先于消息落库）；抛出走消息重试，耗尽后由兜底扫描收敛
            throw new IllegalStateException("出票消息先于订单行到达 orderNo=" + message.getOrderNo());
        }
        if (!OrderStatusEnum.QUEUING.getCode().equals(order.getStatus())) {
            LOG.info("订单已非出票中状态，放弃出票 orderNo={}, status={}", order.getOrderNo(), order.getStatus());
            return;
        }
        // 2. DB 行锁选座 + 贪心分配（站序已由生产者校验，随消息透传）
        int need = message.getPassengers().size();
        List<DailyTrainSeat> availableSeats = dailyTrainSeatMapper.selectAllAvailableForUpdate(
                message.getDailyTrainId(), message.getDepartIndex(), message.getArriveIndex(), message.getSeatType());
        List<DailyTrainSeat> allocated = seatAllocationStrategy.allocate(availableSeats, message.getSeatType(), need);
        if (CollUtil.isEmpty(allocated) || allocated.size() < need) {
            // 确定性失败：余票耗尽（缓存-DB 漂移或真已售罄），由消费者终态化并回补，不重试
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_NOT_ENOUGH);
        }
        // 3. 插明细 + CAS 出票中→待支付
        long now = System.currentTimeMillis();
        for (int i = 0; i < need; i++) {
            DailyTrainSeat seat = allocated.get(i);
            OrderCreateMessage.Passenger passenger = message.getPassengers().get(i);
            TrainOrderItem item = new TrainOrderItem();
            item.setId(IdUtil.getSnowflakeNextId());
            item.setOrderId(order.getId());
            item.setPassengerId(passenger.getPassengerId());
            item.setPassengerName(passenger.getName());
            item.setIdCard(passenger.getIdCard());
            item.setDailyTrainSeatId(seat.getId());
            item.setSeatType(message.getSeatType());
            item.setPrice(message.getUnitPrice());
            item.setDepartIndex(message.getDepartIndex());
            item.setArriveIndex(message.getArriveIndex());
            item.setCreateTime(new Date(now));
            item.setUpdateTime(new Date(now));
            trainOrderItemMapper.insert(item);
        }
        int updated = trainOrderMapper.updateStatusIfMatch(order.getId(),
                OrderStatusEnum.QUEUING.getCode(), OrderStatusEnum.PENDING.getCode(), null, null);
        if (updated == 0) {
            // 状态已被兜底扫描改动：明细随本事务回滚，无泄漏
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_CONCURRENT_CONFLICT);
        }
        // 4. 事务提交后发延时关单消息（发送失败仅告警：兜底扫描会在过期后关单）
        registerCloseDelayAfterCommit(order.getId());
        LOG.info("异步出票成功 orderNo={}, memberId={}, items={}", order.getOrderNo(), order.getMemberId(), need);
    }

    /**
     * 出票失败终态化：CAS 出票中→出票失败，CAS 成功者负责回补预扣余票并清除预扣上下文
     *（CAS 失败说明状态已被兜底扫描处理，跳过回补防双补偿）。
     * 供消费端确定性失败与兜底扫描共用。
     *
     * @param orderId     订单ID
     * @param dailyTrainId 排班ID
     * @param seatType    座位类型
     * @param departIndex 出发站序
     * @param arriveIndex 到达站序
     * @param need        预扣数量
     * @return true 本次完成终态化+回补；false 状态已被其他路径处理
     */
    public boolean failQueuingOrder(Long orderId, Long dailyTrainId, String seatType,
                                    Integer departIndex, Integer arriveIndex, int need) {
        int updated = trainOrderMapper.updateStatusIfMatch(orderId,
                OrderStatusEnum.QUEUING.getCode(), OrderStatusEnum.FAILED.getCode(), null, null);
        if (updated == 0) {
            return false;
        }
        ticketCacheService.incrRemaining(dailyTrainId, seatType, departIndex, arriveIndex, need);
        redissonClient.getBucket(PRE_DECR_KEY_PREFIX + orderId).delete();
        return true;
    }

    /**
     * 关单单笔核心（延时消息消费与兜底扫描共用）：CAS 待支付→已取消 → 删明细释放区间占用 →
     * 事务提交后回补 Redis 余票。CAS 幂等：已支付/已取消/不存在的订单空转返回 false，
     * 与支付 CAS（待支付→已支付）竞态时两者只有一个能成功。
     *
     * @param orderId 订单ID
     * @return true 实际关单；false 状态不符（幂等空转）
     */
    @Transactional
    public boolean closeOrder(Long orderId) {
        TrainOrder order = trainOrderMapper.selectById(orderId);
        if (ObjectUtil.isNull(order) || !OrderStatusEnum.PENDING.getCode().equals(order.getStatus())) {
            return false;
        }
        int updated = trainOrderMapper.updateStatusIfMatch(orderId,
                OrderStatusEnum.PENDING.getCode(), OrderStatusEnum.CANCELLED.getCode(), null, null);
        if (updated == 0) {
            return false;
        }
        // 先取回补参数（读明细）再删明细，原因同 cancel：删后读恒为空会导致余票永不回补
        releaseRemainingAfterCommit(order);
        trainOrderItemMapper.deleteByOrderId(orderId);
        LOG.info("关单成功 orderNo={}, memberId={}", order.getOrderNo(), order.getMemberId());
        return true;
    }

    /**
     * 兜底扫描（每 5 分钟）：消息可靠性的最后防线，两类收敛——
     * 1. 待支付超时关单：延时消息丢失/消费失败时由扫描完成 PENDING 过期关单（与延时消息 CAS 幂等互补）；
     * 2. 出票中悬挂单：出票消息丢失/重试耗尽进死信后，按预扣上下文置出票失败并回补余票。
     * <p>与整点对账任务无冲突：本任务只动订单行与缓存回补，不做缓存重建。
     *
     * @return 本次收敛的订单数
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public int sweepTimeoutOrders() {
        int count = 0;
        // 1) PENDING 超时关单
        List<TrainOrder> expiredOrders = trainOrderMapper.selectExpiredPending(new Date());
        for (TrainOrder order : expiredOrders) {
            if (self.closeOrder(order.getId())) {
                count++;
            }
        }
        // 2) 出票中悬挂单：出票窗口已过（expire_time 再放宽 5 分钟，容忍正常的消费延迟）
        Date threshold = Date.from(LocalDateTime.now().minusMinutes(5).toInstant(java.time.ZoneOffset.ofHours(8)));
        List<TrainOrder> hungOrders = trainOrderMapper.selectHungQueuing(threshold);
        for (TrainOrder order : hungOrders) {
            if (compensateHungQueuing(order)) {
                count++;
            }
        }
        if (count > 0) {
            LOG.info("兜底扫描收敛订单 {} 笔", count);
        }
        return count;
    }

    /**
     * 悬挂出票中订单收敛：按 Redis 预扣上下文回补余票；上下文已超期丢失时仅置失败并告警，
     * 余票由整点对账任务按 DB 区间占用模型最终修正
     */
    private boolean compensateHungQueuing(TrainOrder order) {
        String ctxJson = (String) redissonClient.getBucket(PRE_DECR_KEY_PREFIX + order.getId()).get();
        if (ObjectUtil.isNull(ctxJson)) {
            LOG.warn("悬挂出票中订单缺少预扣上下文（已超期），余票交由对账修正 orderNo={}", order.getOrderNo());
            return trainOrderMapper.updateStatusIfMatch(order.getId(),
                    OrderStatusEnum.QUEUING.getCode(), OrderStatusEnum.FAILED.getCode(), null, null) > 0;
        }
        JSONObject ctx = JSONUtil.parseObj(ctxJson);
        boolean done = failQueuingOrder(order.getId(), ctx.getLong("dailyTrainId"), ctx.getStr("seatType"),
                ctx.getInt("departIndex"), ctx.getInt("arriveIndex"), ctx.getInt("need"));
        if (done) {
            LOG.info("悬挂出票中订单已终态化并回补 orderNo={}", order.getOrderNo());
        }
        return done;
    }

    /**
     * 注册事务提交后发送延时关单消息的回调（同步/异步出票共用）
     */
    private void registerCloseDelayAfterCommit(Long orderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendCloseDelayMessage(orderId);
                }
            });
        } else {
            sendCloseDelayMessage(orderId);
        }
    }

    /**
     * 发送延时关单消息：延时等级可配（RocketMQ 4.x 固定 18 级，默认 14=10min，
     * 与订单支付过期时间对齐）。消费端 CAS 幂等：已支付/已关单的消息天然空转。
     * 发送失败仅告警——兜底扫描会在过期后完成关单，不因消息通道故障拖垮下单事务。
     *
     * @param orderId 订单ID
     */
    private void sendCloseDelayMessage(Long orderId) {
        try {
            rocketMQTemplate.syncSend(orderMqConfig.getCloseTopic(),
                    MessageBuilder.withPayload(String.valueOf(orderId)).build(),
                    3000, orderMqConfig.getCloseDelayLevel());
        } catch (Exception e) {
            LOG.error("延时关单消息发送失败（兜底扫描会接管） orderId={}, error={}", orderId, e.getMessage());
        }
    }

    /**
     * 生成订单号：日期(8位) + 雪花ID后10位
     *
     * @param now 当前时间戳
     * @return 订单号
     */
    private String genOrderNo(long now) {
        String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date(now));
        String snow = String.valueOf(IdUtil.getSnowflakeNextId());
        return date + snow.substring(Math.max(0, snow.length() - 10));
    }

    /**
     * 查询会员订单列表（含明细，按下单时间倒序）
     *
     * @param memberId 会员ID
     * @return 订单列表
     */
    public List<OrderQueryResp> queryByMemberId(Long memberId) {
        List<TrainOrder> orders = trainOrderMapper.selectByMemberId(memberId);
        List<OrderQueryResp> respList = new ArrayList<>();
        if (CollUtil.isNotEmpty(orders)) {
            for (TrainOrder order : orders) {
                respList.add(buildQueryResp(order));
            }
        }
        return respList;
    }

    /**
     * 查询订单详情（含明细）
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    public OrderQueryResp queryByOrderNo(String orderNo) {
        TrainOrder order = trainOrderMapper.selectByOrderNo(orderNo);
        if (ObjectUtil.isNull(order)) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_ORDER_NOT_EXIST);
        }
        return buildQueryResp(order);
    }

    /**
     * 组装订单查询响应（订单头 + 明细）
     *
     * @param order 订单实体
     * @return 订单查询响应
     */
    private OrderQueryResp buildQueryResp(TrainOrder order) {
        OrderQueryResp resp = new OrderQueryResp();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setMemberId(order.getMemberId());
        resp.setTrainId(order.getTrainId());
        resp.setDepartStationId(order.getDepartStationId());
        resp.setArriveStationId(order.getArriveStationId());
        resp.setRunDate(order.getRunDate());
        resp.setStatus(order.getStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setPayTime(order.getPayTime());
        resp.setRefundTime(order.getRefundTime());
        resp.setCreateTime(order.getCreateTime());
        List<TrainOrderItem> items = trainOrderItemMapper.selectByOrderId(order.getId());
        if (CollUtil.isNotEmpty(items)) {
            List<OrderQueryResp.OrderItemResp> itemResps = new ArrayList<>();
            for (TrainOrderItem item : items) {
                OrderQueryResp.OrderItemResp itemResp = new OrderQueryResp.OrderItemResp();
                itemResp.setPassengerName(item.getPassengerName());
                itemResp.setIdCard(item.getIdCard());
                itemResp.setSeatType(item.getSeatType());
                itemResp.setPrice(item.getPrice());
                itemResp.setDepartIndex(item.getDepartIndex());
                itemResp.setArriveIndex(item.getArriveIndex());
                itemResps.add(itemResp);
            }
            resp.setItems(itemResps);
        }
        return resp;
    }
}
