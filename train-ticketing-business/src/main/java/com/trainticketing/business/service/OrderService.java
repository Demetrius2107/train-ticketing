package com.trainticketing.business.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 下单：为每个乘车人分配一个区间可售座位，生成订单 + 订单明细（事务）。
     * 余票校验基于区间占用模型：可售座位 = sale_status='0' 且未被区间重叠的已支付订单占用。
     * 高并发防超卖：先 Redis Lua 原子预扣区间余票，失败即返回余票不足；订单取消/超时再回补。
     *
     * @param req 下单请求
     * @return 订单号
     */
    @Transactional
    public String save(OrderSaveReq req) {
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
        int need = req.getPassengers().size();
        // 1. Redis Lua 原子预扣区间余票（按乘车人数），防并发超卖
        long remainAfter = ticketCacheService.decrRemaining(
            req.getDailyTrainId(), req.getSeatType(), depart.getStationIndex(), arrive.getStationIndex(), need);
        if (remainAfter < 0) {
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_SEAT_NOT_ENOUGH);
        }
        // 2. DB 兜底校验可售座位（缓存与 DB 一致性防线）
        List<DailyTrainSeat> availableSeats = dailyTrainSeatMapper.selectAvailableByInterval(
            req.getDailyTrainId(), depart.getStationIndex(), arrive.getStationIndex(),
            req.getSeatType(), need);
        if (CollUtil.isEmpty(availableSeats) || availableSeats.size() < need) {
            // 预扣已成功但 DB 不足（异常数据兜底）：回补缓存后拒绝
            ticketCacheService.incrRemaining(
                req.getDailyTrainId(), req.getSeatType(), depart.getStationIndex(), arrive.getStationIndex(), need);
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
        // 回补 Redis 区间余票（按订单明细的区间+座位类型+数量）
        releaseRemaining(order);
        // 删除明细：释放座位区间占用（余票恢复）
        trainOrderItemMapper.deleteByOrderId(order.getId());
        // 置订单为已取消
        TrainOrder update = new TrainOrder();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.CANCELLED.getCode());
        trainOrderMapper.updateById(update);
        LOG.info("取消订单成功 orderNo={}, memberId={}", orderNo, order.getMemberId());
    }

    /**
     * 回补订单占用的 Redis 区间余票：按订单明细的区间（departIndex/arriveIndex）、
     * 座位类型与明细数量，将余票加回缓存（取消/超时释放占用时调用）。
     *
     * @param order 订单实体
     */
    private void releaseRemaining(TrainOrder order) {
        List<TrainOrderItem> items = trainOrderItemMapper.selectByOrderId(order.getId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        TrainOrderItem first = items.get(0);
        ticketCacheService.incrRemaining(order.getDailyTrainId(), first.getSeatType(),
                                               first.getDepartIndex(), first.getArriveIndex(), items.size());
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
