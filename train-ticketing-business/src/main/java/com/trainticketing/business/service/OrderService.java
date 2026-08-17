package com.trainticketing.business.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
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
import com.trainticketing.business.req.OrderSaveReq;
import com.trainticketing.business.resp.OrderQueryResp;
import com.trainticketing.common.exception.BusinessException;
import com.trainticketing.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>Title: OrderService</p>
 * <p>Description: 订单服务：下单（校验排班/区间 → 分配可售座位 → 生成订单+明细，含区间占用）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
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

    /** 自身代理引用：下单需先加分布式锁（非事务）再进入事务方法，避免同类自调用导致 @Transactional 失效 */
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

    /** 幂等缓存 key 前缀：order:idem:{memberId}:{idempotentKey} */
    private static final String IDEM_KEY_PREFIX = "order:idem:";
    /** 幂等占位有效期：5 分钟，覆盖下单+支付窗口 */
    private static final java.time.Duration IDEM_TTL = java.time.Duration.ofMinutes(5);

    /**
     * 幂等占位：SETNX 语义，key 存在返回 false（重复提交），否则占位返回 true。
     * 用 Redisson RBucket.setIfAbsent 实现，与下单链路共用 RedissonClient。
     *
     * @param memberId       会员ID
     * @param idempotentKey  幂等键（前端生成）
     * @return true 首次提交可继续；false 重复提交
     */
    private boolean acquireIdempotent(Long memberId, String idempotentKey) {
        String key = IDEM_KEY_PREFIX + memberId + ":" + idempotentKey;
        return redissonClient.getBucket(key).setIfAbsent("1", IDEM_TTL);
    }

    /**
     * 下单事务体：Redis 预扣 + DB 选座（行锁兜底）+ 生成订单/明细。
     * 由 {@link #save} 持分布式锁后通过 self 代理调用，保证 @Transactional 代理生效。
     *
     * @param req         下单请求
     * @param dailyTrain  排班（锁外已查）
     * @param depart      出发经停站
     * @param arrive      到达经停站
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
        // 2. DB 兜底校验可售座位（FOR UPDATE 行锁，与缓存一致性防线）；不足抛异常 → 事务回滚 → 回调回补
        List<DailyTrainSeat> availableSeats = dailyTrainSeatMapper.selectAvailableForUpdate(
            req.getDailyTrainId(), depart.getStationIndex(), arrive.getStationIndex(),
            req.getSeatType(), need);
        if (CollUtil.isEmpty(availableSeats) || availableSeats.size() < need) {
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
            DailyTrainSeat seat = availableSeats.get(i);
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
        // CAS 成功后删除明细：释放座位区间占用（余票恢复）
        trainOrderItemMapper.deleteByOrderId(order.getId());
        // 缓存-DB 一致性：回补 Redis 余票放至事务提交后，避免回滚导致缓存虚高超卖
        releaseRemainingAfterCommit(order);
        LOG.info("取消订单成功 orderNo={}, memberId={}", orderNo, order.getMemberId());
    }

    /**
     * 回补订单占用的 Redis 区间余票：按订单明细的区间（departIndex/arriveIndex）、
     * 座位类型与明细数量，将余票加回缓存。
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
     * @param orderNo 订单号
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
        // CAS 成功后删除明细：释放座位区间占用（余票恢复）
        trainOrderItemMapper.deleteByOrderId(order.getId());
        // 缓存-DB 一致性：回补 Redis 余票放至事务提交后（与取消一致）
        releaseRemainingAfterCommit(order);
        LOG.info("退票成功 orderNo={}, memberId={}", orderNo, order.getMemberId());
    }

    /**
     * 超时关单（定时任务/手动触发）：将已超过支付过期时间的待支付订单批量置为已取消，
     * 并删除明细释放区间占用（余票恢复）。
     *
     * @return 本次关单的订单数
     */
    @Transactional
    public int expirePendingOrders() {
        List<TrainOrder> expiredOrders = trainOrderMapper.selectExpiredPending(new Date());
        if (CollUtil.isEmpty(expiredOrders)) {
            return 0;
        }
        int count = 0;
        for (TrainOrder order : expiredOrders) {
            // CAS 状态转换：PENDING→CANCELLED，先判状态再删明细。
            // 若用户刚支付成功（CAS 失败），跳过该订单，不删明细不回补。
            int updated = trainOrderMapper.updateStatusIfMatch(order.getId(),
                OrderStatusEnum.PENDING.getCode(), OrderStatusEnum.CANCELLED.getCode(), null, null);
            if (updated == 0) {
                LOG.info("超时关单跳过（订单状态已变更） orderNo={}", order.getOrderNo());
                continue;
            }
            // CAS 成功后再删除明细：释放座位区间占用
            trainOrderItemMapper.deleteByOrderId(order.getId());
            // 缓存-DB 一致性：回补 Redis 余票放至事务提交后
            releaseRemainingAfterCommit(order);
            count++;
            LOG.info("超时关单 orderNo={}, memberId={}", order.getOrderNo(), order.getMemberId());
        }
        return count;
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
